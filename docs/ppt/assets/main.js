// ───────────────────────────────────────────────────────────
// AI Mafia Game — Tech PPT
// Three.js ambient background + GSAP entrances + interactions
// ───────────────────────────────────────────────────────────

import * as THREE from "three";

/* ═══════════════════════════════════════════════════════════
   1) Three.js — ambient node ring (dimmed; overlay handles legibility)
   ═══════════════════════════════════════════════════════════ */
(function initBackground() {
  const canvas = document.getElementById("bg-canvas");
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 100);
  camera.position.set(0, 0, 20);

  const factionColors = [
    new THREE.Color(0xf56565), // mafia
    new THREE.Color(0xf56565),
    new THREE.Color(0x6aa6ff), // citizen
    new THREE.Color(0x4ddbc4), // police
    new THREE.Color(0x6ee79f), // doctor
    new THREE.Color(0xc094f7), // psycho
  ];

  const nodeCount = 6;
  const radius = 6;
  const nodes = [];
  const positions = [];

  for (let i = 0; i < nodeCount; i++) {
    const angle = (i / nodeCount) * Math.PI * 2;
    const x = Math.cos(angle) * radius;
    const y = Math.sin(angle) * radius * 0.55;
    const z = (Math.random() - 0.5) * 4;
    positions.push(new THREE.Vector3(x, y, z));

    const mesh = new THREE.Mesh(
      new THREE.IcosahedronGeometry(0.55, 0),
      new THREE.MeshBasicMaterial({
        color: factionColors[i],
        wireframe: true,
        transparent: true,
        opacity: 0.35,
      })
    );
    mesh.position.copy(positions[i]);
    nodes.push(mesh);
    scene.add(mesh);
  }

  // Connecting lines (faint)
  const lineGeo = new THREE.BufferGeometry();
  const linePositions = [];
  for (let i = 0; i < nodeCount; i++) {
    for (let j = i + 1; j < nodeCount; j++) {
      linePositions.push(positions[i].x, positions[i].y, positions[i].z);
      linePositions.push(positions[j].x, positions[j].y, positions[j].z);
    }
  }
  lineGeo.setAttribute("position", new THREE.Float32BufferAttribute(linePositions, 3));
  const lines = new THREE.LineSegments(
    lineGeo,
    new THREE.LineBasicMaterial({ color: 0xf5c46b, transparent: true, opacity: 0.04 })
  );
  scene.add(lines);

  // Particles
  const pCount = 280;
  const pGeo = new THREE.BufferGeometry();
  const pPos = new Float32Array(pCount * 3);
  for (let i = 0; i < pCount; i++) {
    pPos[i * 3]     = (Math.random() - 0.5) * 50;
    pPos[i * 3 + 1] = (Math.random() - 0.5) * 30;
    pPos[i * 3 + 2] = (Math.random() - 0.5) * 20 - 5;
  }
  pGeo.setAttribute("position", new THREE.BufferAttribute(pPos, 3));
  const particles = new THREE.Points(
    pGeo,
    new THREE.PointsMaterial({ color: 0xffffff, size: 0.04, transparent: true, opacity: 0.3 })
  );
  scene.add(particles);

  const mouse = { x: 0, y: 0, tx: 0, ty: 0 };
  window.addEventListener("pointermove", (e) => {
    mouse.tx = (e.clientX / window.innerWidth) * 2 - 1;
    mouse.ty = -((e.clientY / window.innerHeight) * 2 - 1);
  });

  const clock = new THREE.Clock();
  function tick() {
    const t = clock.getElapsedTime();
    mouse.x += (mouse.tx - mouse.x) * 0.05;
    mouse.y += (mouse.ty - mouse.y) * 0.05;
    nodes.forEach((n, i) => {
      n.rotation.x = t * 0.4 + i;
      n.rotation.y = t * 0.3 + i * 0.5;
      n.position.z = Math.sin(t * 0.5 + i) * 1.2;
    });
    lines.rotation.z = Math.sin(t * 0.1) * 0.05;
    particles.rotation.y = t * 0.015;

    camera.position.x = mouse.x * 1.5;
    camera.position.y = mouse.y * 1.0;
    camera.lookAt(0, 0, 0);

    renderer.render(scene, camera);
    requestAnimationFrame(tick);
  }
  tick();

  window.addEventListener("resize", () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });
})();

/* ═══════════════════════════════════════════════════════════
   2) Reveal.js init (transition off — GSAP handles all motion)
   ═══════════════════════════════════════════════════════════ */
const deck = new Reveal({
  hash: true,
  controls: true,
  progress: true,
  slideNumber: "c/t",
  transition: "none",
  backgroundTransition: "none",
  width: 1600,
  height: 900,
  margin: 0.04,
  minScale: 0.35,
  maxScale: 1.4,
  keyboard: true,
  touch: true,
  overview: true,
});
deck.initialize();

/* ═══════════════════════════════════════════════════════════
   3) GSAP — slide entrance animations
   ═══════════════════════════════════════════════════════════ */
const ease = "power3.out";

function animateSlide(section) {
  const items = section.querySelectorAll("[data-anim]");

  const groups = {
    fadeup: [], "fadeup-delay": [], "fadeup-late": [],
    cardin: [], flowstep: [],
  };
  items.forEach((el) => {
    const k = el.dataset.anim;
    (groups[k] || groups.fadeup).push(el);
  });

  const tl = gsap.timeline({ defaults: { ease } });
  if (groups.fadeup.length)
    tl.fromTo(groups.fadeup, { opacity: 0, y: 30 }, { opacity: 1, y: 0, duration: 0.7, stagger: 0.06 }, 0);
  if (groups["fadeup-delay"].length)
    tl.fromTo(groups["fadeup-delay"], { opacity: 0, y: 60 }, { opacity: 1, y: 0, duration: 1.0 }, 0.2);
  if (groups["fadeup-late"].length)
    tl.fromTo(groups["fadeup-late"], { opacity: 0, y: 20 }, { opacity: 1, y: 0, duration: 0.8 }, 0.6);
  if (groups.cardin.length)
    tl.fromTo(groups.cardin,
      { opacity: 0, y: 36, scale: 0.96 },
      { opacity: 1, y: 0, scale: 1, duration: 0.7, stagger: 0.08 }, 0.15);
  if (groups.flowstep.length)
    tl.fromTo(groups.flowstep,
      { opacity: 0, scale: 0.9, y: 24 },
      { opacity: 1, scale: 1, y: 0, duration: 0.6, stagger: 0.15 }, 0.1);

  // Hero title — whole-line fadeup (covers text-nodes that span queries miss)
  if (section.classList.contains("s-title")) revealHero(section);

  // Number countup (slide "What" — .bs-n)
  countupNumbers(section);

  // Reset card transforms left over from previous visit
  section.querySelectorAll(".cast-card, .part, .tech, .cmp, .why-li, .rm, .fb")
    .forEach((c) => gsap.set(c, { clearProps: "transform" }));

  // Reset any card selection state from previous visit
  section.querySelectorAll(".is-selected, .is-dimmed").forEach((el) => {
    el.classList.remove("is-selected", "is-dimmed");
    gsap.set(el, { scale: 1, opacity: 1 });
  });
}

deck.on("ready", (e) => animateSlide(e.currentSlide));
deck.on("slidechanged", (e) => animateSlide(e.currentSlide));

/* ─── Hero entrance (whole line, not split — text nodes were stranded) ─ */
function revealHero(section) {
  const hero = section.querySelector(".t-hero");
  if (!hero) return;
  gsap.fromTo(hero,
    { opacity: 0, y: 50 },
    { opacity: 1, y: 0, duration: 1.0, ease, delay: 0.15 });
  // Italic accent gets a small extra lift
  const accent = hero.querySelector(".t-italic");
  if (accent) {
    gsap.fromTo(accent,
      { y: 30, opacity: 0, skewY: 8 },
      { y: 0, opacity: 1, skewY: 0, duration: 1.0, ease, delay: 0.6 });
  }
}

/* ─── Number countup (no flash — set to 0 before tween) ──────────────── */
function countupNumbers(section) {
  section.querySelectorAll(".bs-n").forEach((el) => {
    const target = parseInt(el.dataset.target ?? el.textContent.trim(), 10);
    if (Number.isNaN(target)) return;
    el.dataset.target = String(target);
    el.textContent = "0";  // prevent flash of final value
    const obj = { v: 0 };
    gsap.to(obj, {
      v: target, duration: 1.4, ease: "power2.out", delay: 0.5,
      onUpdate() { el.textContent = Math.round(obj.v); },
    });
  });
}

/* ═══════════════════════════════════════════════════════════
   4) Global interactions — hover lift, click focus, cursor glow
   ═══════════════════════════════════════════════════════════ */

/* ─── Cursor glow (subtle, follows pointer) ─────────────── */
(function cursorGlow() {
  const dot = document.createElement("div");
  dot.className = "cursor-glow";
  document.body.appendChild(dot);
  let x = window.innerWidth / 2, y = window.innerHeight / 2;
  let tx = x, ty = y;
  window.addEventListener("pointermove", (e) => { tx = e.clientX; ty = e.clientY; });
  function loop() {
    x += (tx - x) * 0.12;
    y += (ty - y) * 0.12;
    dot.style.transform = `translate(${x}px, ${y}px)`;
    requestAnimationFrame(loop);
  }
  loop();
})();

/* ─── Card hover lift + click focus (siblings dim) ─────── */
const interactiveSelectors = [
  ".cast-card", ".part", ".tech", ".cmp", ".why-li", ".rm", ".fb",
];
const groupSelectors = [
  ".cast", ".parts", ".techs", ".compare", ".why-list", ".rm-list", ".flow-big",
];

function bindCard(card) {
  if (card.dataset.bound === "1") return;
  card.dataset.bound = "1";

  card.addEventListener("mouseenter", () => {
    if (card.classList.contains("is-dimmed")) return;
    gsap.to(card, { y: -8, scale: 1.025, duration: 0.32, ease: "power3.out" });
  });
  card.addEventListener("mouseleave", () => {
    if (card.classList.contains("is-selected")) return;
    gsap.to(card, { y: 0, scale: 1, duration: 0.45, ease: "power3.out" });
  });

  card.addEventListener("click", (e) => {
    e.stopPropagation();
    const group = card.parentElement;
    const siblings = Array.from(group.children).filter(
      (n) => interactiveSelectors.some((s) => n.matches?.(s))
    );

    const wasSelected = card.classList.contains("is-selected");

    // Reset all first
    siblings.forEach((s) => {
      s.classList.remove("is-selected", "is-dimmed");
      gsap.to(s, { opacity: 1, scale: 1, y: 0, duration: 0.35, ease: "power3.out" });
    });

    if (!wasSelected) {
      card.classList.add("is-selected");
      gsap.to(card, { scale: 1.06, y: -10, duration: 0.45, ease: "power3.out" });
      siblings.forEach((s) => {
        if (s !== card) {
          s.classList.add("is-dimmed");
          gsap.to(s, { opacity: 0.32, scale: 0.97, duration: 0.4, ease: "power3.out" });
        }
      });
    }
  });
}

// Click outside any card → clear selection in current slide
document.addEventListener("click", (e) => {
  if (e.target.closest(interactiveSelectors.join(","))) return;
  document.querySelectorAll(".is-selected, .is-dimmed").forEach((el) => {
    el.classList.remove("is-selected", "is-dimmed");
    gsap.to(el, { opacity: 1, scale: 1, y: 0, duration: 0.35, ease: "power3.out" });
  });
});

// Bind on every slide change (DOM is reused but new selectors may appear)
function bindAllCards() {
  document.querySelectorAll(interactiveSelectors.join(",")).forEach(bindCard);
}
deck.on("ready", bindAllCards);
deck.on("slidechanged", bindAllCards);

/* ─── Keyboard helpers ──────────────────────────────────── */
document.addEventListener("keydown", (e) => {
  // 'C' to clear card selection manually
  if (e.key === "c" || e.key === "C") {
    document.querySelectorAll(".is-selected, .is-dimmed").forEach((el) => {
      el.classList.remove("is-selected", "is-dimmed");
      gsap.to(el, { opacity: 1, scale: 1, y: 0, duration: 0.3 });
    });
  }
});
