const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source=fs.readFileSync('src/main/resources/static/js/evidencia-diseno.js','utf8');
async function run() {
  const input={value:'Mi explicación'}; const status={textContent:''};
  const form={querySelector(selector){return selector==='textarea'?input:status;}};
  let handler; const button={dataset:{type:'CIRCUITO'},closest(){return form;},addEventListener(_,fn){handler=fn;}};
  const sandbox={document:{querySelectorAll(){return [button];}},window:{LabDesigns:{async request(){return {voltage:5,parts:[{a:1,b:0,r:220}]};}}}};
  vm.runInNewContext(source,sandbox); await handler();
  assert.match(input.value,/Mi explicación/); assert.match(input.value,/R1: \+ a −, 220 Ω/);
  assert.match(status.textContent,/Revisa el texto/);
  input.value='x'.repeat(4999); await handler(); assert.equal(input.value.length,4999); assert.match(status.textContent,/espacio disponible/);
  console.log('Evidencia de diseño: importación y límite aprobados.');
}
run().catch(error=>{console.error(error);process.exitCode=1;});
