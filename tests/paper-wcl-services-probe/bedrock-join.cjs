'use strict'
// Genuine isolated Geyser connection, all advertised pack bytes checked before
// completing the resource stack. Not Xbox/Floodgate authentication or rendering.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createHash } = require('node:crypto')
const { Client } = require('bedrock-protocol')
const expected=process.argv[2]
assert.match(expected,/^[0-9a-f-]{36}$/)
const amounts=[1,5,10,50,100,500,1000,5000,10000]
const client=new Client({host:'127.0.0.1',port:19132,version:'1.26.30',
  transport:'raknet',raknetBackend:'raknet-native',offline:true,username:'ProbeBedrock',
  deviceOS:1,autoInitPlayer:true,delayedInit:true,connectTimeout:15000})
const pending=new Map(), verified=new Set(), ids=new Map(), inventory=new Map()
let info, chunks=[], finished=false, spawned=false
const deadline=setTimeout(()=>fail(new Error('Product Bedrock acceptance timeout')),90000)
function fail(e){if(finished)return;finished=true;clearTimeout(deadline);console.error(e);try{client.close()}finally{process.exit(1)}}
function guard(fn){return p=>{try{fn(p)}catch(e){fail(e)}}}
function sha(b){return createHash('sha256').update(b).digest()}
function wireUuid(id){
  const b=Buffer.from(id.replace(/-/g,''),'hex')
  return Buffer.concat([Buffer.from(b.subarray(0,8)).reverse(),Buffer.from(b.subarray(8)).reverse()]).toString('hex')
}
function packId(p){return p.pack_id.split('_')[0]}
client.on('error',fail)
client.on('kick',p=>fail(new Error('Server rejected fixture: '+p.message)))
client.on('close',()=>{if(!finished)fail(new Error('Premature disconnect'))})
client.on('network_stack_latency',p=>{if(p.needs_response)client.queue('network_stack_latency',{timestamp:p.timestamp,needs_response:false})})
client.once('resource_packs_info',guard(p=>{
  let total=0
  for(const pack of p.texture_packs){
    const id=pack.content_identity
    assert.match(id,/^[0-9a-f-]{36}$/)
    assert.equal(pack.uuid.replace(/-/g,'').toLowerCase(),wireUuid(id))
    assert(!pending.has(id));assert.equal(pack.cdn_url,'')
    const size=Number(pack.size)
    assert(size>0&&size<=32*1024*1024)
    total+=size;pending.set(id,pack)
  }
  assert(total<=64*1024*1024&&pending.has(expected))
  assert.equal(pending.get(expected).version,'1.0.0')
  client.write('resource_pack_client_response',{response_status:'send_packs',resourcepackids:[...pending].map(([id,p])=>id+'_'+p.version)})
}))
client.on('resource_pack_data_info',guard(p=>{
  assert(!info,'overlapping pack metadata')
  const id=packId(p),entry=pending.get(id)
  assert(entry&&!verified.has(id))
  assert.equal(Number(p.size),Number(entry.size))
  assert(p.max_chunk_size>0&&p.chunk_count>0&&p.chunk_count<4096)
  assert.equal(p.chunk_count,Math.ceil(Number(p.size)/p.max_chunk_size))
  assert.equal(Buffer.from(p.hash).length,32)
  info=p;chunks=[]
  client.write('resource_pack_chunk_request',{pack_id:info.pack_id,chunk_index:0})
}))
client.on('resource_pack_chunk_data',guard(p=>{
  assert(info);const id=packId(info)
  assert.equal(packId(p),id);assert.equal(p.chunk_index,chunks.length)
  const bytes=Buffer.from(p.payload)
  assert(bytes.length>0&&bytes.length<=info.max_chunk_size)
  chunks.push(bytes)
  if(chunks.length<info.chunk_count){client.write('resource_pack_chunk_request',{pack_id:info.pack_id,chunk_index:chunks.length});return}
  const result=Buffer.concat(chunks)
  assert.equal(result.length,Number(info.size));assert(sha(result).equals(Buffer.from(info.hash)))
  if(id===expected)fs.writeFileSync('bedrock-received.mcpack',result,{flag:'wx'})
  verified.add(id);info=undefined;chunks=[]
  if(verified.size===pending.size){
    console.log('NETWORK_BEDROCK_ALL_PACKS_VERIFIED '+verified.size)
    client.write('resource_pack_client_response',{response_status:'have_all_packs',resourcepackids:[]})
  }
}))
client.once('resource_pack_stack',guard(p=>{
  assert.deepEqual(p.resource_packs.map(x=>x.uuid).sort(),[...verified].sort())
  client.write('resource_pack_client_response',{response_status:'completed',resourcepackids:[]})
  client.queue('client_cache_status',{enabled:false})
}))
client.once('start_game',()=>client.queue('request_chunk_radius',{chunk_radius:4,max_radius:4}))
client.on('item_registry',guard(p=>{
  for(const amount of amounts){
    const name='simplecoins:coin_'+amount
    const matches=p.itemstates.filter(s=>s.name===name)
    assert.equal(matches.length,1,name)
    ids.set(amount,matches[0].runtime_id)
  }
  assert.equal(new Set(ids.values()).size,9)
  console.log('NETWORK_BEDROCK_NINE_REGISTRY_IDS_VERIFIED')
}))
client.once('spawn',guard(()=>{
  spawned=true
  assert.equal(ids.size,9)
  console.log('NETWORK_BEDROCK_PRODUCT_SPAWN')
  checkCoins()
}))
function checkCoins(){
  if(!spawned||ids.size!==9)return
  for(const id of ids.values()){
    if([...inventory.values()].filter(x=>x.network_id===id).reduce((n,x)=>n+x.count,0)!==1)return
  }
  console.log('NETWORK_BEDROCK_NINE_REAL_INVENTORY_COINS_PASSED')
  finished=true;clearTimeout(deadline);client.close();process.exit(0)
}
client.on('inventory_content',guard(p=>{
  if(p.window_id!=='inventory'&&p.window_id!==0)return
  inventory.clear();p.input.forEach((item,slot)=>inventory.set(slot,item));checkCoins()
}))
client.on('inventory_slot',guard(p=>{
  if(p.window_id!=='inventory'&&p.window_id!==0)return
  inventory.set(p.slot,p.item);checkCoins()
}))
client.once('connect_allowed',()=>client.connection.ping(5000).then(p=>{
  assert.match(String(p),/^MCPE;/);client.connect()
}).catch(fail))
try{client.init()}catch(e){fail(e)}
