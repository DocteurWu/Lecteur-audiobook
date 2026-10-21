// Génère les icônes PNG de l'application (mipmap) — casque blanc sur dégradé violet.
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const resDir = process.argv[2];
const script = path.join(resDir, '..', 'tools', 'make-icons.ps1');
const sizes = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };

for (const [dpi, size] of Object.entries(sizes)) {
  const dir = path.join(resDir, 'mipmap-' + dpi);
  fs.mkdirSync(dir, { recursive: true });
  const out = path.join(dir, 'ic_launcher.png');
  execFileSync('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script, '-Size', String(size), '-Out', out], {
    stdio: 'inherit',
  });
  console.log('icône', dpi, size + 'px');
}
