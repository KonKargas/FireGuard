document.addEventListener('DOMContentLoaded', () => {
  const svg = document.getElementById('locationPreviewSvg');
  const form = document.querySelector('.location-main-form');
  if (!svg || !form) return;
  const floor = form.querySelector('[name="floorId"]:not([type="hidden"])') || form.querySelector('[name="floorId"]');
  const fields = Object.fromEntries(['x','y','z','width','depth','height','code'].map(k => [k, form.querySelector(`[name="${k}"]`)]));
  const state = document.getElementById('locationPreviewState');
  const help = document.getElementById('locationPreviewHelp');
  const currentId = form.dataset.editId ? String(form.dataset.editId) : null;
  const NS = 'http://www.w3.org/2000/svg';
  let existing = [], loadedFloor = null, requestId = 0;
  const number = (key) => Number(fields[key]?.value);
  const finite = (n) => Number.isFinite(n);
  const overlap = (a,b) => a.x < b.x+b.width && a.x+a.width > b.x && a.y < b.y+b.depth && a.y+a.depth > b.y && a.z < b.z+b.height && a.z+a.height > b.z;
  function node(tag, attrs, parent=svg) {const el=document.createElementNS(NS,tag);for(const [k,v] of Object.entries(attrs)) el.setAttribute(k,String(v));parent.append(el);return el;}
  function setState(label,kind='') {state.textContent=label;state.className='location-mini-status'+(kind?' is-'+kind:'');}
  function render() {
    svg.replaceChildren();
    if (!loadedFloor) { node('text',{x:260,y:180,'text-anchor':'middle',fill:'currentColor'}).textContent='Select a floor to display its spaces';svg.setAttribute('viewBox','0 0 520 380'); return; }
    const candidate = {x:number('x'),y:number('y'),z:number('z'),width:number('width'),depth:number('depth'),height:number('height')};
    const valid = [candidate.x,candidate.y,candidate.z,candidate.width,candidate.depth,candidate.height].every(finite) && candidate.width>0 && candidate.depth>0 && candidate.height>0;
    const spaces=existing.filter(r=>String(r.id)!==currentId);
    const conflicts=valid?spaces.filter(r=>overlap(candidate,r)):[];
    const rects=valid?[...spaces,candidate]:spaces;
    const minX=Math.min(0,...rects.map(r=>r.x))-1.5, minY=Math.min(0,...rects.map(r=>r.y))-1.5;
    const maxX=Math.max(10,...rects.map(r=>r.x+r.width))+1.5,maxY=Math.max(8,...rects.map(r=>r.y+r.depth))+1.5;
    const rangeX=Math.max(2,maxX-minX), rangeY=Math.max(2,maxY-minY);
    const step=36, pad=34,w=rangeX*step+pad*2,h=rangeY*step+pad*2;
    svg.setAttribute('viewBox',`0 0 ${w} ${h}`);
    const px=x=>pad+(x-minX)*step, py=y=>pad+(y-minY)*step;
    for(let x=Math.ceil(minX);x<=maxX;x++){node('line',{x1:px(x),y1:pad,x2:px(x),y2:h-pad,stroke:'currentColor','stroke-opacity':'.11'});}
    for(let y=Math.ceil(minY);y<=maxY;y++){node('line',{x1:pad,y1:py(y),x2:w-pad,y2:py(y),stroke:'currentColor','stroke-opacity':'.11'});}
    spaces.forEach(r=>{
      const conflicted=conflicts.some(c=>c.id===r.id);
      const rect=node('rect',{x:px(r.x),y:py(r.y),width:r.width*step,height:r.depth*step,rx:2,fill:conflicted?'#e75c57':'#699bb8','fill-opacity':'.25',stroke:conflicted?'#e75c57':'#699bb8','stroke-width':2});
      const t=node('title',{},rect);t.textContent=`${r.code} — ${r.name} (${r.width} × ${r.depth} m)`;
      if(r.width>=1.3&&r.depth>=0.6){const label=node('text',{x:px(r.x+r.width/2),y:py(r.y+r.depth/2)+4,'text-anchor':'middle',fill:'currentColor','font-size':Math.min(14,Math.max(9,r.width*3.2)),'pointer-events':'none'});label.textContent=r.code||r.name;}
    });
    if(valid) {
      const bad=conflicts.length>0;
      const rect=node('rect',{x:px(candidate.x),y:py(candidate.y),width:candidate.width*step,height:candidate.depth*step,rx:2,fill:bad?'#e75c57':'#31b997','fill-opacity':'.34',stroke:bad?'#e75c57':'#31b997','stroke-width':3,'stroke-dasharray':'9 5'});
      node('title',{},rect).textContent='Proposed location';
      const label=node('text',{x:px(candidate.x+candidate.width/2),y:py(candidate.y+candidate.depth/2)+5,'text-anchor':'middle',fill:'currentColor','font-weight':'bold','font-size':14,'pointer-events':'none'});
      label.textContent=fields.code?.value?.trim()||'NEW';
      setState(bad?'Overlap detected':'No overlap',bad?'error':'ok');
      help.textContent=bad?`Conflicts with: ${conflicts.map(r=>r.code).join(', ')}. Move the proposed space by editing X/Y or its dimensions.`:'Green outline: proposed location fits without overlapping existing 3D spaces. The server validates again when saving.';
    } else {setState('Check dimensions','error');help.textContent='Enter valid numeric coordinates and positive dimensions to display the proposed space.';}
  }
  async function loadFloor() {
    const id=floor?.value;const myRequest=++requestId;
    if (!id){existing=[];loadedFloor=null;setState('Select a floor');render();return;}
    setState('Loading floor…');
    try {
      const res=await fetch(`/locations/floor-preview?floorId=${encodeURIComponent(id)}`,{headers:{Accept:'application/json'}});
      if(!res.ok)throw Error(`HTTP ${res.status}`);
      const data=await res.json();if(myRequest!==requestId)return;
      existing=Array.isArray(data)?data:[];loadedFloor=id;render();
    }catch(e){if(myRequest!==requestId)return;existing=[];loadedFloor=null;render();setState('Could not load floor','error');help.textContent='Floor preview unavailable. Saving still uses server-side validation.';}
  }
  floor?.addEventListener('change',loadFloor);
  Object.values(fields).forEach(el=>el?.addEventListener('input',render));
  loadFloor();
});
