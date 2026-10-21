const fs = require('fs');
const path = require('path');
const dir = process.argv[2];
const p = path.join(dir, 'Library.java');
let s = fs.readFileSync(p, 'utf8');
if (!s.includes('import java.util.Collections;')) {
  s = s.replace('import java.util.ArrayList;', 'import java.util.ArrayList;\nimport java.util.Collections;');
}
fs.writeFileSync(p, s, 'utf8');
console.log('ok Library.java');
