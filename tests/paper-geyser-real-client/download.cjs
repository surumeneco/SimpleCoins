// Real Geyser UDP pack download on a disposable loopback server.
// No graphical rendering or authenticated Floodgate login is asserted.
'use strict'
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { createHash } = require('node:crypto')
const { Client } = require('bedrock-protocol')
const [expected, output] = process.argv.slice(2)
assert.match(expected, /^[0-9a-f-]{36}$/)
assert.equal(output, 'bedrock-received.mcpack')
const client = new Client({
  host: '127.0.0.1', port: 19132, version: '1.26.30',
  transport: 'raknet', raknetBackend: 'raknet-native',
  offline: true, username: 'SimpleCoinsBedrock', deviceOS: 1,
  autoInitPlayer: false, delayedInit: true, connectTimeout: 15000
})
let announced, info, chunks = [], finished = false
const deadline = setTimeout(() => fail(new Error('Bedrock packet transfer timeout')), 45000)
function fail (error) {
  if (finished) return
  finished = true
  clearTimeout(deadline)
  console.error(error)
  try { client.close() } finally { process.exit(1) }
}
function guarded (handler) {
  return packet => { try { handler(packet) } catch (error) { fail(error) } }
}
client.on('error', fail)
client.on('kick', p => fail(new Error('Server rejected fixture: ' + p.message)))
client.on('close', () => { if (!finished) fail(new Error('Disconnected before complete pack')) })
client.on('network_stack_latency', p => {
  if (p.needs_response) client.queue('network_stack_latency', {timestamp:p.timestamp,needs_response:false})
})
client.once('resource_packs_info', guarded(packet => {
  const matches = packet.texture_packs.filter(p => p.uuid === expected)
  assert.equal(matches.length, 1, 'SimpleCoins pack advertised exactly once')
  announced = matches[0]
  assert.equal(announced.version,'1.0.0')
  assert.equal(announced.cdn_url, '', 'expect real Geyser chunk delivery')
  assert(Number(announced.size)>0 && Number(announced.size)<=2*1024*1024)
  client.write('resource_pack_client_response', {
    response_status:'send_packs',resourcepackids:[expected+'_'+announced.version]
  })
}))
client.once('resource_pack_data_info', guarded(packet => {
  assert(announced, 'download must follow advertisement')
  assert.equal(packet.pack_id.split('_')[0], expected)
  assert.equal(Number(packet.size), Number(announced.size))
  assert(packet.max_chunk_size > 0)
  assert(packet.chunk_count > 0 && packet.chunk_count < 128)
  assert.equal(packet.chunk_count, Math.ceil(Number(packet.size)/packet.max_chunk_size))
  assert.equal(Buffer.from(packet.hash).length, 32)
  info=packet
  client.write('resource_pack_chunk_request',{pack_id:info.pack_id,chunk_index:0})
}))
client.on('resource_pack_chunk_data', guarded(packet => {
  assert(info, 'chunk before metadata')
  assert.equal(packet.pack_id.split('_')[0], expected)
  assert.equal(packet.chunk_index,chunks.length)
  const payload=Buffer.from(packet.payload)
  assert(payload.length>0 && payload.length<=info.max_chunk_size)
  chunks.push(payload)
  if(chunks.length<info.chunk_count){
    client.write('resource_pack_chunk_request',{pack_id:info.pack_id,chunk_index:chunks.length})
    return
  }
  const result=Buffer.concat(chunks)
  assert.equal(result.length,Number(info.size))
  const hash=createHash('sha256').update(result).digest()
  assert(hash.equals(Buffer.from(info.hash)), 'transferred archive hash mismatch')
  fs.writeFileSync(output,result,{flag:'wx'})
  console.log('NETWORK_BEDROCK_PACK_CHUNKS_VERIFIED bytes='+result.length+' sha256='+hash.toString('hex'))
  // Stop here; not all server packs have been downloaded and no renderer is running.
  finished=true
  clearTimeout(deadline)
  client.close()
  process.exit(0)
}))
client.once('connect_allowed', () => {
  // Establish actual unconnected ping/pong first, then use the modern
  // version-aware RakNet 11 backend. No Microsoft authentication is used.
  client.connection.ping(5000).then(pong => {
    assert.match(String(pong), /^MCPE;/)
    console.log('NETWORK_BEDROCK_RAKNET_PONG')
    const send = client.write.bind(client)
    client.write = (name, packet) => {
      console.log('BEDROCK_SEND', name)
      return send(name, packet)
    }
    client.on('packet', p => console.log('BEDROCK_RECEIVE', p.data?.name ?? p.name ?? 'unknown'))
    client.connect()
  }).catch(fail)
})
try { client.init() } catch (error) { fail(error) }
