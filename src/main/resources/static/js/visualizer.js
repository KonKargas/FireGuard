import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

const viewer = document.getElementById('viewer');
const picker = document.getElementById('buildingPicker');
const msg = document.getElementById('vizMessage');
const stats = document.getElementById('vizStats');
const floorPanel = document.getElementById('floorToggles');
const inspector = document.getElementById('roomInspector');
const sensorsCheckbox = document.getElementById('showSensors');
const labelsCheckbox = document.getElementById('showLabels');
const shellCheckbox = document.getElementById('showShell');
const detailsCheckbox = document.getElementById('showArchitecture');
const explodedCheckbox = document.getElementById('explodeFloors');

const scene = new THREE.Scene();
scene.background = new THREE.Color('#0d1724');
const camera = new THREE.PerspectiveCamera(45, 1, 0.1, 5000);
const renderer = new THREE.WebGLRenderer({antialias:true, alpha:false});
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.16;
viewer.appendChild(renderer.domElement);
const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.075;
controls.maxPolarAngle = Math.PI * 0.89;
scene.add(new THREE.HemisphereLight(0xd9edff, 0x293648, 2.0));
const sun = new THREE.DirectionalLight(0xffffff, 2.0);
sun.position.set(20,40,15); scene.add(sun);
const content = new THREE.Group(); scene.add(content);
const raycaster = new THREE.Raycaster();
const pointer = new THREE.Vector2();
let pickables = [];
let floorGroups = new Map();
let sensorObjects = [];
let labelObjects = [];
let activeData = null;
let cameraFocus = new THREE.Vector3();
let shellObjects = [];
let architectureObjects = [];
let selectedRoomId = null;
let highlightMeshes = [];
let currentBounds = new THREE.Box3();
let isolatedFloorId = null;
const FLOOR_COLORS = [0x3c708c,0x587d92,0x4b8191,0x54768b,0x668799];
const mat = (color,opacity=1)=>new THREE.MeshStandardMaterial({color,roughness:.82,metalness:.05,transparent:opacity<1,opacity,depthWrite:opacity===1,side:THREE.DoubleSide});
function block(parent,w,h,d,x,y,z,color,opacity=1){
  const mesh=new THREE.Mesh(new THREE.BoxGeometry(Math.max(.025,w),Math.max(.025,h),Math.max(.025,d)),mat(color,opacity));
  mesh.position.set(x,y,z);parent.add(mesh);return mesh;
}
function line(parent,points,color=0x9fbbcd,opacity=.65){
  const geo=new THREE.BufferGeometry().setFromPoints(points.map(p=>new THREE.Vector3(...p)));
  const edge=new THREE.Line(geo,new THREE.LineBasicMaterial({color,transparent:true,opacity}));parent.add(edge);return edge;
}
// Draw walls with crisp outlines and a cutaway height so rooms remain visible.
function wallSegment(parent,x1,z1,x2,z2,y,height,color=0xc5d6e1,opacity=.72,thickness=.095){
  const length=Math.hypot(x2-x1,z2-z1); if(length<.08)return;
  const wall=block(parent,length,height,thickness,(x1+x2)/2,y+height/2,(z1+z2)/2,color,opacity);
  wall.rotation.y=-Math.atan2(z2-z1,x2-x1);
  const edges=new THREE.LineSegments(new THREE.EdgesGeometry(wall.geometry),
    new THREE.LineBasicMaterial({color:0x698497,transparent:true,opacity:.48}));
  edges.position.copy(wall.position); edges.rotation.copy(wall.rotation);parent.add(edges);
}
// Doorway is a VISUAL convention; no door position is stored in the database.
function wallWithDoorway(parent,x1,z1,x2,z2,y,height,color){
  const length=Math.hypot(x2-x1,z2-z1);
  const doorWidth=Math.min(.95,length*.36), doorHeight=Math.min(1.95,height*.87);
  if(length<2.2 || height<1.8){wallSegment(parent,x1,z1,x2,z2,y,height,color);return;}
  const dx=(x2-x1)/length,dz=(z2-z1)/length;
  const left=(length-doorWidth)/2, right=(length+doorWidth)/2;
  wallSegment(parent,x1,z1,x1+dx*left,z1+dz*left,y,height,color);
  wallSegment(parent,x1+dx*right,z1+dz*right,x2,z2,y,height,color);
  wallSegment(parent,x1+dx*left,z1+dz*left,x1+dx*right,z1+dz*right,y+doorHeight,height-doorHeight,color);
  // Floor threshold makes the opening evident from an elevated camera.
  wallSegment(parent,x1+dx*left,z1+dz*left,x1+dx*right,z1+dz*right,y,.025,0x778fa0,.95,.12);
}
function roomColor(room, fallback){
 const type=String(room.type || room.locationType || '').toUpperCase();
 if(type.includes('BATH') || type.includes('WC'))return 0x55a39f;
 if(type.includes('CORRIDOR') || type.includes('HALL'))return 0xc4a06b;
 if(type.includes('STAIR'))return 0x8897a8;
 if(type.includes('OFFICE'))return 0x9279b5;
 if(type.includes('KITCHEN'))return 0xb88e65;
 if(type.includes('LOBBY') || type.includes('RECEPTION'))return 0x7798aa;
 if(type.includes('STORAGE'))return 0x84928d;
 return fallback;
}
function addRoomOutline(parent,room,y,color){
 const x=room.x,z=room.y,w=room.width,d=room.depth;
 const points=[[x,y,z],[x+w,y,z],[x+w,y,z+d],[x,y,z+d],[x,y,z]];
 line(parent,points,color,.95);
}

function addPillar(group,x,z,yBase,height){
  block(group,.18,height,.18,x,yBase+height/2,z,0xa1bac9,.94);
}
function floorExtents(rooms){
  if(!rooms.length)return null;
  const minX=Math.min(...rooms.map(r=>r.x)),maxX=Math.max(...rooms.map(r=>r.x+r.width));
  const minZ=Math.min(...rooms.map(r=>r.y)),maxZ=Math.max(...rooms.map(r=>r.y+r.depth));
  return {minX:minX-.5,maxX:maxX+.5,minZ:minZ-.5,maxZ:maxZ+.5};
}
function refreshFloorVisibility(){
  floorGroups.forEach((group,id)=>{
    const check=floorPanel.querySelector(`input[data-floor-id="${id}"]`);
    group.visible=Boolean(check?.checked)&&(isolatedFloorId===null||isolatedFloorId===id);
  });
}
function updateExplode(){
  const all=[...floorGroups.values()].sort((a,b)=>a.userData.level-b.userData.level);
  const offsets=new Map(all.map((g,index)=>[g.userData.id,explodedCheckbox.checked?index*1.75:0]));
  for(const group of all)group.position.y=offsets.get(group.userData.id);
}
function updateShell(){shellObjects.forEach(obj=>obj.visible=shellCheckbox.checked);}
function updateArchitecture(){architectureObjects.forEach(obj=>obj.visible=detailsCheckbox.checked);}


function clearModel() {
  for (const o of [...content.children]) {
    content.remove(o);
    o.traverse(child => {
      if (child.geometry) child.geometry.dispose();
      if (child.material) (Array.isArray(child.material)?child.material:[child.material]).forEach(m=>m.dispose());
      if (child.userData.labelTexture) child.userData.labelTexture.dispose();
    });
  }
  floorGroups.clear(); sensorObjects = []; labelObjects = []; pickables = []; shellObjects = []; architectureObjects = []; highlightMeshes = []; selectedRoomId = null; isolatedFloorId = null;
  floorPanel.replaceChildren(); inspector.textContent = 'Click a room or sensor to inspect it.';
}
function textSprite(text) {
  const canvas = document.createElement('canvas');
  canvas.width = 1024;
  canvas.height = 160;
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = 'rgba(13,23,36,.82)';
  ctx.fillRect(0, 14, canvas.width, 132);
  ctx.fillStyle = '#ffffff';
  ctx.font = 'bold 56px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  // maxWidth scales long labels rather than clipping the room name.
  ctx.fillText(text, canvas.width / 2, canvas.height / 2, canvas.width - 48);
  const texture = new THREE.CanvasTexture(canvas);
  const material = new THREE.SpriteMaterial({map:texture,transparent:true,depthTest:false});
  const sprite = new THREE.Sprite(material);
  sprite.scale.set(3.8, .60, 1);
  sprite.userData.labelTexture = texture;
  return sprite;
}
function renderBuilding(data) {
  clearModel(); activeData=data;
  const bounds = new THREE.Box3();
  let roomsCount=0,sensorsCount=0;
  const floors=[...data.floors].sort((a,b)=>a.elevation-b.elevation);
  floors.forEach((floor,index)=>{
    const group=new THREE.Group();group.name=`Floor ${floor.level}`;
    group.userData={id:String(floor.id),level:floor.level};content.add(group);
    floorGroups.set(String(floor.id),group);
    const entry=document.createElement('div');entry.className='viz-floor-entry';
    const check=document.createElement('input');check.type='checkbox';check.checked=true;check.dataset.floorId=String(floor.id);
    check.addEventListener('change',refreshFloorVisibility);
    const title=document.createElement('strong');title.textContent=`${floor.name} (Level ${floor.level})`;
    const count=document.createElement('span');count.textContent=`${floor.rooms.length} rooms`;
    const isolate=document.createElement('button');isolate.type='button';isolate.className='viz-isolate';isolate.textContent='Isolate';
    isolate.addEventListener('click',()=>{
      isolatedFloorId=isolatedFloorId===String(floor.id)?null:String(floor.id);
      floorPanel.querySelectorAll('.viz-isolate').forEach(b=>b.classList.remove('active'));
      if(isolatedFloorId)isolate.classList.add('active');else isolate.textContent='Isolate';
      refreshFloorVisibility();
    });
    entry.append(check,title,count,isolate);floorPanel.append(entry);
    const ext=floorExtents(floor.rooms);
    if(!ext)return;
    const minRoomZ=Math.min(...floor.rooms.map(r=>r.z));
    const y=floor.elevation+minRoomZ;
    const maxRoomHeight=Math.max(...floor.rooms.map(r=>r.height+r.z-minRoomZ));
    const w=ext.maxX-ext.minX,d=ext.maxZ-ext.minZ;
    const cx=(ext.minX+ext.maxX)/2,cz=(ext.minZ+ext.maxZ)/2;
    const floorColor=FLOOR_COLORS[index%FLOOR_COLORS.length];
    // Architectural storey: slab, perimeter foundation, columns, partial exterior shell and cornice.
    const slab=block(group,w,.24,d,cx,y-.14,cz,0x718798,1);
    // Recessed floor inset adds a visible slab border around the plan.
    block(group,Math.max(.05,w-.20),.023,Math.max(.05,d-.20),cx,y-.006,cz,0x93a8b7,.94);
    const slabOutline=new THREE.LineSegments(new THREE.EdgesGeometry(slab.geometry),new THREE.LineBasicMaterial({color:0xdce7ec,transparent:true,opacity:.75}));
    slabOutline.position.copy(slab.position);group.add(slabOutline);
    const architecture=new THREE.Group();group.add(architecture);shellObjects.push(architecture);
    const architecturalDetails=new THREE.Group(); group.add(architecturalDetails); architectureObjects.push(architecturalDetails);
    const halfWall=Math.max(maxRoomHeight*.72,1.6);
    // Back and left facades are glazed/translucent: front and right remain open (cutaway).
    block(architecture,w,halfWall,.095,cx,y+halfWall/2,ext.minZ,0x80a5b8,.19);
    block(architecture,.095,halfWall,d,ext.minX,y+halfWall/2,cz,0x80a5b8,.19);
    for(const [xx,zz] of [[ext.minX,ext.minZ],[ext.maxX,ext.minZ],[ext.minX,ext.maxZ],[ext.maxX,ext.maxZ]]){
      addPillar(architecture,xx,zz,y,maxRoomHeight);
    }
    for(const zz of [ext.minZ,ext.maxZ])line(architecture,[[ext.minX,y+maxRoomHeight,zz],[ext.maxX,y+maxRoomHeight,zz]],0xa5c5d7,.72);
    for(const xx of [ext.minX,ext.maxX])line(architecture,[[xx,y+maxRoomHeight,ext.minZ],[xx,y+maxRoomHeight,ext.maxZ]],0xa5c5d7,.72);
    // A roof cap is only used at the top storey and may be hidden with the shell.
    if(index===floors.length-1){
      // Roof outline rather than solid glass canopy: top rooms remain readable.
      for(const zz of [ext.minZ,ext.maxZ])line(architecture,[[ext.minX,y+maxRoomHeight+.10,zz],[ext.maxX,y+maxRoomHeight+.10,zz]],0xd1e4ef,.95);
    }
    for(const room of floor.rooms){
      roomsCount++;
      const x=room.x,z=room.y,yy=floor.elevation+room.z,h=room.height;
      const warning=room.activeAlert;
      const roomGroup=new THREE.Group(); group.add(roomGroup);
      const finish=warning?0xd75456:roomColor(room,floorColor);
      const base=block(roomGroup,room.width,.048,room.depth,x+room.width/2,yy+.035,z+room.depth/2,finish,warning?.82:.88);
      base.userData={kind:'room',room,floor};pickables.push(base);
      const outlineGroup=new THREE.Group();roomGroup.add(outlineGroup);architectureObjects.push(outlineGroup);
      addRoomOutline(outlineGroup,room,yy+.068,warning?0xff8e83:0xcee1ec);
      // Thin cutaway walls: front segment contains an illustrative doorway.
      const wallHeight=Math.max(.45,Math.min(h*.67,2.15));
      const walls=new THREE.Group();roomGroup.add(walls);architectureObjects.push(walls);
      wallSegment(walls,x,z,x+room.width,z,yy,wallHeight,warning?0xf59b98:0xc5d5e1,.72);
      wallSegment(walls,x,z,x,z+room.depth,yy,wallHeight,warning?0xf59b98:0xc5d5e1,.72);
      wallSegment(walls,x+room.width,z,x+room.width,z+room.depth,yy,wallHeight,warning?0xf59b98:0xb0c5d4,.50);
      wallWithDoorway(walls,x,z+room.depth,x+room.width,z+room.depth,yy,wallHeight,warning?0xf59b98:0xb0c5d4);
      // Invisible volume for selecting a room without obscuring its interior.
      const volume=new THREE.Mesh(new THREE.BoxGeometry(room.width,h,room.depth),new THREE.MeshBasicMaterial({visible:false,side:THREE.DoubleSide}));
      volume.position.set(x+room.width/2,yy+h/2,z+room.depth/2);
      volume.userData={kind:'room',room,floor};roomGroup.add(volume);pickables.push(volume);
      if(warning){
        const pulse=block(roomGroup,room.width,.014,room.depth,x+room.width/2,yy+.065,z+room.depth/2,0xff3d4d,.38);
        pulse.userData={kind:'room',room,floor};pickables.push(pulse);
      }
      const label=textSprite([room.code, room.name].filter(Boolean).join(' - '));
      label.position.set(x+room.width/2,yy+Math.min(h*.57,1.45),z+room.depth/2);
      labelObjects.push(label);group.add(label);
      for(const sensor of room.sensors){
        sensorsCount++;
        const isTemp=sensor.type.toUpperCase().includes('TEMP');
        const dot=new THREE.Mesh(new THREE.SphereGeometry(.15,16,12),new THREE.MeshStandardMaterial({color:isTemp?0xff7653:0xf6cf59,emissive:isTemp?0x6b2414:0x5a470b,emissiveIntensity:.75}));
        dot.position.set(x+sensor.x,yy+sensor.z,z+sensor.y);
        dot.userData={kind:'sensor',room,floor,sensor};group.add(dot);pickables.push(dot);sensorObjects.push(dot);
      }
    }
    bounds.expandByPoint(new THREE.Vector3(ext.minX,y-.2,ext.minZ));
    bounds.expandByPoint(new THREE.Vector3(ext.maxX,y+maxRoomHeight+.3,ext.maxZ));
  });
  currentBounds.copy(bounds);
  stats.textContent=`${data.floors.length} floors · ${roomsCount} rooms · ${sensorsCount} sensors`;
  msg.textContent=roomsCount?'':'No rooms have been created in this building yet.';
  updateShell();updateArchitecture();updateExplode();refreshFloorVisibility();
  sensorObjects.forEach(s=>s.visible=sensorsCheckbox.checked);
  labelObjects.forEach(s=>s.visible=labelsCheckbox.checked);
  if(roomsCount)fitCamera(bounds);
}
function fitCamera(bounds){
  const size=bounds.getSize(new THREE.Vector3());
  const center=bounds.getCenter(new THREE.Vector3());
  cameraFocus.copy(center); controls.target.copy(center);
  const distance=Math.max(size.length()*1.1,10);
  camera.position.set(center.x+distance,center.y+distance*.75,center.z+distance);
  camera.near=Math.max(.01,distance/10000);camera.far=Math.max(1000,distance*30);
  camera.updateProjectionMatrix();controls.update();
}
function inspect(item){
  inspector.replaceChildren();
  const {room,floor}=item.userData;
  const heading=document.createElement('div');heading.className='viz-room-title';heading.textContent=`${room.code} · ${room.name}`;inspector.append(heading);
  const p=document.createElement('p');p.textContent=`${floor.name} · Position (${room.x}, ${room.y}, ${room.z}) m`;inspector.append(p);
  const dims=document.createElement('p');dims.textContent=`Dimensions: ${room.width} × ${room.depth} × ${room.height} m`;inspector.append(dims);
  const status=document.createElement('p');status.textContent=room.activeAlert?'⚠ Active simulated fire alert':'No active fire alert';inspector.append(status);
  if(item.userData.kind==='sensor'){
    const sensor=item.userData.sensor;
    const detail=document.createElement('p');detail.textContent=`Selected sensor: ${sensor.code} (${sensor.type}) · Reliability ${(sensor.reliability*100).toFixed(2)}%`;inspector.append(detail);
  }
  const list=document.createElement('div');list.className='viz-sensor-list';
  room.sensors.forEach(s=>{const line=document.createElement('div');line.textContent=`${s.type}: ${s.code}`;list.append(line)});
  inspector.append(list);
  const link=document.createElement('a');link.href=`/locations/${encodeURIComponent(room.id)}`;link.textContent='Open Location Details';inspector.append(link);
}
let down=null;
renderer.domElement.addEventListener('pointerdown',e=>{down={x:e.clientX,y:e.clientY}});
renderer.domElement.addEventListener('pointerup',e=>{
  if(!down || Math.hypot(e.clientX-down.x,e.clientY-down.y)>5){down=null;return;}down=null;
  const rect=renderer.domElement.getBoundingClientRect();
  pointer.x=((e.clientX-rect.left)/rect.width)*2-1;pointer.y=-((e.clientY-rect.top)/rect.height)*2+1;
  raycaster.setFromCamera(pointer,camera);
  const visible=pickables.filter(o=>{let p=o;while(p){if(!p.visible)return false;p=p.parent;}return true;});
  const hit=raycaster.intersectObjects(visible,false)[0];if(hit)inspect(hit.object);
});
async function load(){
  const id=picker.value;clearModel();activeData=null;
  if(!id){msg.textContent='Select a building to begin.';stats.textContent='No building loaded';return;}
  msg.textContent='Loading building…';
  try {
    const response=await fetch(`/visualizer/api/buildings/${encodeURIComponent(id)}`,{headers:{Accept:'application/json'}});
    if(!response.ok) throw new Error(`Server returned ${response.status}`);
    const data=await response.json();if(picker.value!==id)return;renderBuilding(data);
  }catch(err){msg.textContent=`Unable to load building: ${err.message}`;console.error(err);}
}
picker.addEventListener('change',load);
document.getElementById('refreshView').addEventListener('click',load);
document.getElementById('resetView').addEventListener('click',()=>{if(activeData&&!currentBounds.isEmpty())fitCamera(currentBounds)});
document.getElementById('showAllFloors').addEventListener('click',()=>{
 isolatedFloorId=null;
 floorPanel.querySelectorAll('input').forEach(input=>input.checked=true);
 floorPanel.querySelectorAll('.viz-isolate').forEach(b=>b.classList.remove('active'));
 refreshFloorVisibility();
});
sensorsCheckbox.addEventListener('change',()=>sensorObjects.forEach(obj=>obj.visible=sensorsCheckbox.checked));
labelsCheckbox.addEventListener('change',()=>labelObjects.forEach(obj=>obj.visible=labelsCheckbox.checked));
shellCheckbox.addEventListener('change',updateShell);
detailsCheckbox.addEventListener('change',updateArchitecture);
explodedCheckbox.addEventListener('change',updateExplode);
function resize(){const w=viewer.clientWidth,h=viewer.clientHeight;if(w&&h){camera.aspect=w/h;camera.updateProjectionMatrix();renderer.setSize(w,h,false)}}
const observer=new ResizeObserver(resize);observer.observe(viewer);resize();
renderer.setAnimationLoop(()=>{controls.update();renderer.render(scene,camera)});
if(picker.options.length===2){picker.selectedIndex=1;load();}
