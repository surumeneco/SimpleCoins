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
let info, chunks=[], finished=false, spawned=false, coinsVerified=false, startGame, opened, top=[], cursorId=0
const responses=new Map()
const deadline=setTimeout(()=>fail(new Error('Product Bedrock acceptance timeout')),150000)
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
client.once('start_game',p=>{startGame=p;client.queue('request_chunk_radius',{chunk_radius:4,max_radius:4})})
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
  if(coinsVerified||!spawned||ids.size!==9)return
  for(const id of ids.values()){
    if([...inventory.values()].filter(x=>x.network_id===id).reduce((n,x)=>n+x.count,0)!==1)return
  }
  console.log('NETWORK_BEDROCK_NINE_REAL_INVENTORY_COINS_PASSED')
  coinsVerified=true
  runWallet().catch(fail)
}
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms))
async function awaitCondition(condition,description,ms=15000){
  const until=Date.now()+ms
  while(!condition()){if(Date.now()>until)throw new Error('Missing '+description);await sleep(50)}
}
function stackId(item){
  if(!item||!item.network_id)return 0
  const id=typeof item.stack_id==='object'?item.stack_id.id:item.stack_id
  assert(Number.isInteger(id)&&id>0,'No actual item stack net ID')
  return id
}
function slot(container_id,slot,stack_id){return {slot_type:{container_id},slot,stack_id}}
async function transfer(request_id,type_id,source,destination){
  client.queue('item_stack_request',{requests:[{request_id,actions:[{type_id,count:1,source,destination}],custom_names:[],cause:'chat_public'}]})
  await awaitCondition(()=>responses.has(request_id),'stack response '+request_id)
  const result=responses.get(request_id)
  assert.equal(result.status,'ok',JSON.stringify(result))
  for(const container of result.containers||[]){
    if(container.slot_type.container_id==='cursor'){
      const observation=container.slots.find(x=>x.slot===0)
      if(observation)cursorId=observation.count===0?0:observation.item_stack_id
    }
  }
  console.log('NETWORK_BE_STACK_RESPONSE '+request_id)
  await sleep(600)
}
client.on('container_open',guard(p=>{opened=p;top=[]}))
client.on('item_stack_response',guard(p=>{for(const response of p.responses)responses.set(response.request_id,response)}))
async function runWallet(){
  await awaitCondition(()=>fs.existsSync('product-be-wallet-ready'),'server wallet preparation',30000)
  await awaitCondition(()=>inventory.get(0)?.network_id&&inventory.get(1)?.network_id===ids.get(100)&&inventory.get(1)?.count===1,'actual wallet and 100C hotbar')
  const held=inventory.get(0)
  assert(![...ids.values()].includes(held.network_id)&&held.count===1)
  client.queue('mob_equipment',{runtime_entity_id:startGame.runtime_entity_id,item:held,slot:0,selected_slot:0,window_id:'inventory'})
  await sleep(250)
  const zero={x:0,y:0,z:0}
  client.queue('inventory_transaction',{transaction:{legacy:{legacy_request_id:0},transaction_type:'item_use',actions:[],transaction_data:{action_type:'click_air',trigger_type:'player_input',block_position:zero,face:0,hotbar_slot:0,held_item:held,player_pos:startGame.player_position,click_pos:zero,block_runtime_id:0,client_prediction:'success',client_cooldown_state:'off'}}})
  await awaitCondition(()=>opened&&amounts.every((a,i)=>top[i]?.network_id===ids.get(a)&&top[i]?.count===1),'actual nine wallet icons')
  console.log('NETWORK_BE_WALLET_SCREEN_OBSERVED')
  // Use actual server-issued item IDs and an ordinary cursor transfer.
  await transfer(101,'take',slot('hotbar_and_inventory',1,stackId(inventory.get(1))),slot('cursor',0,0))
  assert(cursorId>0,'cursor stack ID absent')
  await transfer(102,'place',slot('cursor',0,cursorId),slot('container',4,stackId(top[4])))
  console.log('NETWORK_BE_WALLET_DEPOSIT_SENT')
  await awaitCondition(()=>fs.existsSync('product-be-wallet-withdraw'),'server deposit verification',25000)
  await transfer(103,'take',slot('container',4,stackId(top[4])),slot('cursor',0,0))
  assert(cursorId>0,'withdraw cursor stack ID absent')
  await transfer(104,'place',slot('cursor',0,cursorId),slot('hotbar_and_inventory',1,0))
  await awaitCondition(()=>inventory.get(1)?.network_id===ids.get(100)&&inventory.get(1)?.count===1,'withdrawn actual coin')
  client.queue('container_close',{window_id:opened.window_id,window_type:opened.window_type,server:false})
  console.log('NETWORK_BE_WALLET_WITHDRAW_OBSERVED')
  await awaitCondition(()=>fs.existsSync('product-be-wallet-done'),'server withdrawal verification',25000)
  finished=true;clearTimeout(deadline);client.close();process.exit(0)
}
client.on('inventory_content',guard(p=>{
  if(opened&&p.window_id===opened.window_id){top=p.input.slice();return}
  if(p.window_id!=='inventory'&&p.window_id!==0)return
  inventory.clear();p.input.forEach((item,slot)=>inventory.set(slot,item));checkCoins()
}))
client.on('inventory_slot',guard(p=>{
  if(opened&&p.window_id===opened.window_id){top[p.slot]=p.item;return}
  if((p.window_id==='ui'||p.window_id===124)&&p.slot===0){cursorId=stackId(p.item);return}
  if(p.window_id!=='inventory'&&p.window_id!==0)return
  inventory.set(p.slot,p.item);checkCoins()
}))
client.once('connect_allowed',()=>client.connection.ping(5000).then(p=>{
  assert.match(String(p),/^MCPE;/);client.connect()
}).catch(fail))
try{client.init()}catch(e){fail(e)}
