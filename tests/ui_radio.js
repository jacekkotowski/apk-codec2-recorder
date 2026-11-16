// Checks the page against a stub of the Android bridge: Record, RADE and Files tabs, DRED levels, old .c2 files.
// Writes ui_record.png, ui_settings.png, ui_files.png.
// Run: node tests/ui_radio.js
const { chromium } = require('playwright'); const path = require('path');
const STUB = `window.__c=[];window.__t={on:false,done:false,step:'',text:''};window.__radio={on:false,op:'',talk:false,sent:0,frames:0,got:0,needed:0,sync:false,snr:0,level:0,left:0,secs:0,total:0,message:0};
window.Android=new Proxy({},{get:(t,k)=>(...a)=>{__c.push(k+':'+a.join(','));
 if(k=='microphones')return JSON.stringify([{id:7,name:'Phone microphone (bottom)'},{id:9,name:'USB'}]);
 if(k=='list')return JSON.stringify([{name:'2026-11-03_16-10-00_DRED-small.dred',mode:'DRED small',secs:30,bytes:2632,time:2,old:false},{name:'long_DRED-small.dred',mode:'DRED small',secs:300,bytes:19000,time:1,old:false},{name:'2026-11-01_09-00-00_700C.c2',mode:'Codec 2, older version',secs:0,bytes:6407,time:0,old:true}]);
 if(k=='rename')return true;
 if(k=='testState')return JSON.stringify(window.__t);
 if(k=='status')return JSON.stringify({rec:false,paused:false,waiting:false,vad:.3,gain:12,level:.4,secs:0,bytes:0,play:null,pos:0,dur:0,msg:null,sampling:false,hasNoise:true,comp:-1,clip:false,routed:'',radio:window.__radio});
}})`;
(async () => {
  const fails = []; const check = (ok, what) => { if (!ok) { fails.push(what); console.log('FAIL', what); } };
  const b = await chromium.launch(); const pg = await b.newPage({ viewport: { width: 393, height: 800 }, deviceScaleFactor: 2 });
  const errs = []; pg.on('pageerror', e => errs.push(String(e)));
  await pg.addInitScript(STUB); await pg.addInitScript("try{localStorage.setItem('mode','700C')}catch(e){}"); // a mode stored by 2.6
  await pg.goto('file://' + path.resolve(__dirname, '..', 'app', 'assets', 'index.html'));
  const calls = () => pg.evaluate('__c'), vis = s => pg.isVisible(s), set = o => pg.evaluate(o), wait = () => pg.waitForTimeout(450);
  await wait();
  // Record tab: lean by default, settings fold out, recording still starts with the same calls
  check(await vis('#recb') && !await vis('#mic') && !await vis('#live') && !await vis('#files'), 'record tab default');
  check(await pg.innerText('#tabs button[data-t=live]') === 'RADE', 'RADE tab shown');
  check(await pg.inputValue('#mode') === 'small', 'stored Codec 2 mode falls back to DRED small: ' + await pg.inputValue('#mode'));
  const opts = await pg.$$eval('#mode option', o => o.map(x => x.value + '=' + x.textContent));
  check(opts.length === 4 && opts[0].startsWith('best=DRED best') && opts[3].startsWith('smallest='), 'four DRED levels: ' + opts.join(' | '));
  const t0 = await pg.innerText('#list');
  check(t0.includes('Made by an older version') && t0.includes('2026-11-01_09-00-00_700C') && !t0.includes('.dred') && !t0.includes('.c2'), 'old .c2 row and names without extension: ' + t0.replace(/\n/g, ' ').slice(0, 200));
  const oldRow = pg.locator('.r', { hasText: 'older version' });
  check((await oldRow.locator('button').allInnerTexts()).join(',') === 'SHARE,DELETE', 'old .c2 row: share and delete only');
  await oldRow.locator('button:has-text("DELETE")').click(); await oldRow.locator('button:has-text("YES, DELETE FOREVER")').click();
  check((await calls()).includes('remove:2026-11-01_09-00-00_700C.c2'), 'old .c2 delete');
  check(await pg.innerText('#imp') === 'OPEN .DRED', 'import button');
  await pg.screenshot({ path: 'ui_record.png' });
  await pg.click('#adv'); check(await vis('#mic') && await vis('#src') && await vis('#samp') && await vis('#st'), 'settings unfold');
  check(await pg.inputValue('#src') === '6', 'default source is VOICE_RECOGNITION');
  await pg.screenshot({ path: 'ui_settings.png' });
  await pg.click('#adv'); await pg.click('#recb');
  let c = await calls(); check(c.some(x => x.startsWith('startRec:small')), 'startRec small ' + c.filter(x => x.startsWith('startRec')));
  await pg.selectOption('#mode', 'best'); await pg.click('#recb'); c = await calls(); check(c.includes('startRec:best,true,false'), 'startRec best ' + c.filter(x => x.startsWith('startRec'))); check(c.includes('audioConfig:6,0,1,0.18,180,6,12,3'), 'audioConfig defaults unchanged: ' + c.filter(x => x.startsWith('audioConfig')).slice(-1));
  await pg.click('#list .a button:has-text("MORE")'); check(await vis('button:has-text("SAVE WAV")') && await vis('button:has-text("SAVE .DRED")'), 'more menu');
  await pg.click('button:has-text("SAVE .DRED")'); check((await calls()).includes('saveFile:2026-11-03_16-10-00_DRED-small.dred'), 'save .dred');
  await pg.click('button:has-text("RENAME")'); check(await pg.inputValue('#newname') === '2026-11-03_16-10-00_DRED-small', 'rename shows the name without .dred');
  // Files tab
  await pg.click('#tabs button[data-t=files]'); await pg.waitForTimeout(100);
  check(await vis('#frx') && !await vis('#recb') && !await vis('#cmp'), 'files tab');
  let t = await pg.innerText('#list'); check(t.includes('SEND 01:59'), 'send time, 30 s recording, robust: ' + t.replace(/\n/g, ' ').slice(0, 120));
  await pg.selectOption('#rmode', '0'); t = await pg.innerText('#list'); check(t.includes('SEND 00:52'), 'send time fast: ' + t.replace(/\n/g, ' ').slice(0, 120));
  await pg.selectOption('#rmode', '1'); await pg.selectOption('#rex', '100');
  await pg.click('#list .a button:has-text("SEND")'); c = await calls(); check(c.includes('radioSend:2026-11-03_16-10-00_DRED-small.dred,1,100,false'), 'radioSend ' + c.slice(-3));
  await pg.selectOption('#rex', '-1'); await pg.click('#list .a button:has-text("SEND")'); c = await calls(); check(c.includes('radioSend:2026-11-03_16-10-00_DRED-small.dred,1,0,true'), 'radioSend repeat ' + c.slice(-3));
  await pg.selectOption('#rex', '50');
  await pg.click('#fany'); check((await calls()).includes('radioSendFile:1,50,false'), 'radioSendFile ' + (await calls()).slice(-2)); check((await pg.innerText('#rate')).includes('1.4 kB per minute'), 'rate text: ' + await pg.innerText('#rate'));
  await pg.click('#frx'); check((await calls()).includes('radioReceive:1'), 'radioReceive');
  await set("Object.assign(__radio,{on:true,op:'receive',got:9,needed:22,sync:true,snr:6.2,level:.5})"); await wait();
  check((await pg.innerText('#fst')).includes('RECEIVING') && (await pg.innerText('#fsig')).includes('Got 9 of 22') && await vis('#fstop') && !await vis('#frx') && !await vis('#fany'), 'receiving view');
  await pg.screenshot({ path: 'ui_files.png' });
  await pg.click('#fstop'); check((await calls()).includes('radioStop:'), 'radioStop');
  await set("Object.assign(__radio,{on:true,op:'send',sent:12,frames:36,left:80})"); await wait();
  check((await pg.innerText('#fsig')).includes('Frame 12 of 36'), 'sending view');
  await set("Object.assign(__radio,{on:false,op:''})"); await wait();
  check((await pg.innerText('#fst')).includes('READY') && await vis('#frx'), 'ready again');
  // RADE tab: record a message, then play, hear as RADE, send; listen
  await pg.click('#tabs button[data-t=live]'); await wait();
  check(await vis('#rrec') && await vis('#rlisten') && !await vis('#list') && await pg.isDisabled('#rsend') && await pg.isDisabled('#rplay'), 'RADE tab: nothing to send before a message');
  check((await pg.innerText('#lsig')).includes('No message'), 'no message text');
  await pg.click('#rrec'); check((await calls()).includes('radeRecord:true'), 'radeRecord with Denoise ' + (await calls()).slice(-2));
  await set("Object.assign(__radio,{on:true,op:'rade_rec',secs:7,total:60,level:.5})"); await wait();
  check((await pg.innerText('#lst')) === 'RECORDING' && (await pg.innerText('#lsig')).includes('00:07 of 01:00') && await vis('#rstop') && !await vis('#rsend'), 'recording a message');
  await pg.screenshot({ path: 'ui_rade_rec.png' });
  await pg.click('#rstop'); check((await calls()).includes('radioStop:'), 'stop recording');
  await set("Object.assign(__radio,{on:false,op:'',message:12})"); await wait();
  check(!await pg.isDisabled('#rsend') && (await pg.innerText('#lsig')).includes('Message: 00:12'), 'message ready');
  await pg.click('#rplay'); await pg.click('#rhear'); await pg.click('#rsend');
  c = await calls(); check(c.includes('radePlay:false') && c.includes('radePlay:true') && c.includes('radeSend:'), 'play, hear as RADE, send ' + c.slice(-3));
  await set("Object.assign(__radio,{on:true,op:'rade_send',secs:3,total:13,level:.7})"); await wait();
  check((await pg.innerText('#lst')) === 'SENDING' && (await pg.innerText('#lsig')).includes('00:03 / 00:13 on air'), 'sending');
  await set("Object.assign(__radio,{on:false,op:''})"); await wait();
  await pg.check('#ralong'); await pg.click('#rlisten'); check((await calls()).includes('radeListen:true'), 'listen, playing along');
  await set("Object.assign(__radio,{on:true,op:'rade_listen',sync:true,snr:3.4,secs:5,level:.4})"); await wait();
  check((await pg.innerText('#lst')) === 'LISTENING' && (await pg.innerText('#lsig')).includes('Signal locked, SNR 3 dB'), 'listening view');
  await pg.screenshot({ path: 'ui_rade.png' });
  await set("Object.assign(__radio,{on:false,op:'',sync:false})"); await wait();
  // TEST tab: buttons call the bridge, the report is shown, SHARE REPORT waits for a report
  await pg.setViewportSize({ width: 393, height: 800 });
  await pg.click('#tabs button[data-t=test]'); await pg.waitForTimeout(1200);
  check(await vis('#trun') && await vis('#tclip') && !await vis('#recb') && !await vis('#list') && await pg.isDisabled('#tshare') && await pg.isDisabled('#tcopy'), 'test tab default');
  await pg.click('#trun'); await pg.click('#tclip');
  check((await calls()).includes('testRun:') && (await calls()).includes('testClip:'), 'test buttons call the bridge');
  await set("Object.assign(__t,{on:true,step:'Speed: RADE',text:'Self-test\\nPhone: X'})"); await pg.waitForTimeout(1300);
  check((await pg.innerText('#tst')) === 'RUNNING' && await vis('#tstop') && !await vis('#trun') && (await pg.innerText('#trep')).includes('Speed: RADE') && !await pg.isDisabled('#tcopy'), 'test running view, partial report can be copied');
  await set("Object.assign(__t,{on:false,done:true,step:'',text:'Self-test\\nPhone: X\\nEnd of report.'})"); await pg.waitForTimeout(1300);
  check(!await pg.isDisabled('#tshare') && (await pg.innerText('#trep')).includes('End of report.'), 'finished report can be shared');
  await pg.click('#tshare'); check((await calls()).includes('testShare:'), 'share calls the bridge');
  check(!await pg.isDisabled('#tcopy'), 'copy is on once there is a report'); await pg.click('#tcopy'); check((await calls()).includes('testCopy:'), 'copy calls the bridge');
  await pg.screenshot({ path: 'ui_test.png' });
  for (const w of [320, 393]) { // nothing may stick out sideways on a narrow phone
    await pg.setViewportSize({ width: w, height: 700 });
    for (const tb of ['deck', 'live', 'files', 'test']) {
      await pg.click(`#tabs button[data-t=${tb}]`);
      check(await pg.evaluate("document.documentElement.scrollWidth<=innerWidth && [...document.querySelectorAll('.deck:not([hidden]) *')].every(e=>e.getBoundingClientRect().right<=innerWidth+1)"), `fits width ${w} ${tb}`);
    }
  }
  check(!errs.length, 'page errors ' + errs);
  await b.close();
  console.log(fails.length ? fails.length + ' UI FAILURES' : 'UI OK'); process.exit(fails.length ? 1 : 0);
})();
