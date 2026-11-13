(() => {
  const rows = [...document.querySelectorAll('.reading-row')];
  function render(id, type, color, maxY) {
    const canvas = document.getElementById(id);
    if (!canvas) return;
    const values = rows.filter(r=>r.dataset.type===type)
      .map(r=>Number(r.dataset.value)).filter(Number.isFinite).reverse();
    const pixelRatio = window.devicePixelRatio || 1;
    const rect=canvas.getBoundingClientRect();
    const w=Math.max(200,rect.width),h=170;
    canvas.width=Math.round(w*pixelRatio);canvas.height=Math.round(h*pixelRatio);
    const ctx=canvas.getContext('2d');ctx.scale(pixelRatio,pixelRatio);
    ctx.clearRect(0,0,w,h);
    ctx.font='12px system-ui';ctx.fillStyle='#91a0b8';
    if(!values.length){ctx.textAlign='center';ctx.fillText('No measurements available',w/2,h/2);return;}
    const pad={l:37,r:12,t:10,b:24}; const plotW=w-pad.l-pad.r,plotH=h-pad.t-pad.b;
    const upper=Math.max(maxY,Math.ceil(Math.max(...values)/10)*10);
    ctx.strokeStyle='rgba(145,160,184,0.17)';ctx.lineWidth=1;ctx.textAlign='right';
    for(let i=0;i<=4;i++){let y=pad.t+i*plotH/4;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(w-pad.r,y);ctx.stroke();ctx.fillText((upper*(4-i)/4).toFixed(0),pad.l-7,y+4);}
    ctx.beginPath();values.forEach((v,i)=>{const x=pad.l+(values.length===1?plotW/2:i*plotW/(values.length-1));const y=pad.t+plotH-(v/upper)*plotH;i?ctx.lineTo(x,y):ctx.moveTo(x,y)});
    ctx.strokeStyle=color;ctx.lineWidth=2.4;ctx.lineJoin='round';ctx.stroke();
    ctx.fillStyle='#91a0b8';ctx.textAlign='left';ctx.fillText('Older',pad.l,h-6);ctx.textAlign='right';ctx.fillText('Latest',w-pad.r,h-6);
  }
  let frame;
  const draw=()=>{render('tempChart','TEMPERATURE','#f17a61',100);render('lumChart','LUMINOSITY','#e5b44e',200)};
  draw();window.addEventListener('resize',()=>{cancelAnimationFrame(frame);frame=requestAnimationFrame(draw)});
})();
