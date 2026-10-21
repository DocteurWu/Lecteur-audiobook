// Génère les drawables vectoriels de l'app "Chapitres"
const fs = require('fs');
const path = require('path');

const dir = process.argv[2];
fs.mkdirSync(dir, { recursive: true });

const icons = {
  ic_play: 'M8,5v14l11,-7z',
  ic_pause: 'M6,19h4V5H6v14zM14,5v14h4V5h-4z',
  ic_skip_next: 'M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z',
  ic_skip_prev: 'M6,6h2v12H6zM9.5,12l8.5,6V6z',
  ic_speed: 'M20.38,8.57l-1.23,1.85a8,8 0 0 1 -0.22,7.58H5.07A8,8 0 0 1 15.58,6.85l1.85,-1.23A10,10 0 0 0 3.35,19a2,2 0 0 0 1.72,1h13.85a2,2 0 0 0 1.74,-1 10,10 0 0 0 -0.27,-10.44zM10.59,15.41a2,2 0 0 0 2.83,0l5.66,-8.49 -8.49,5.66a2,2 0 0 0 0,2.83z',
  ic_timer: 'M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z',
  ic_search: 'M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z',
  ic_folder_add: 'M20,6h-8l-2,-2H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM14,14v3h-3v-3h-2v3H6v-2h3v-3h2v3h3v2h2v-3h-2z',
  ic_headphones: 'M12,3c-4.97,0 -9,4.03 -9,9v7c0,1.1 0.9,2 2,2h3v-8H5v-1c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v1h-3v8h3c1.1,0 2,-0.9 2,-2v-7c0,-4.97 -4.03,-9 -9,-9z',
  ic_check: 'M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z',
  ic_list: 'M3,13h2v-2H3v2zM3,17h2v-2H3v2zM3,9h2V7H3v2zM7,13h14v-2H7v2zM7,17h14v-2H7v2zM7,7v2h14V7H7z',
  ic_refresh: 'M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -8,8s3.58,8 8,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z',
  ic_more: 'M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z',
  ic_chevron: 'M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z',
  ic_back: 'M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z',
  ic_bookmark: 'M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z',
  ic_alarm_off: 'M20,18.6L3.4,2 2,3.4l3.1,3.1C3.7,8.1 3,9.9 3,12c0,4.4 3.6,8 8,8 2.1,0 3.9,-0.7 5.5,-1.9l2.1,2.1 1.4,-1.4zM11,18c-3.3,0 -6,-2.7 -6,-6 0,-0.6 0.1,-1.2 0.2,-1.7l2.9,2.9 1.4,-1.4L7,8.8C7.6,8.3 8.3,8 9,8h0.3L19,17.7C18.2,18 17.6,18 17,18h-6z',
  ic_bolt: 'M11,21h-1l1,-7H7.5c-0.58,0 -0.57,-0.32 -0.38,-0.66 0.19,-0.34 0.05,-0.08 0.07,-0.12C8.48,10.94 10.42,7.54 13,3h1l-1,7h3.5c0.49,0 0.56,0.33 0.47,0.51l-0.07,0.15C12.96,17.55 11,21 11,21z',
};

for (const [name, d] of Object.entries(icons)) {
  const xml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="@color/text">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="${d}" />
</vector>
`;
  fs.writeFileSync(path.join(dir, name + '.xml'), xml, 'utf8');
}

// fonds
const backgrounds = {
  bg_card: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface" />
    <corners android:radius="16dp" />
    <stroke android:width="1dp" android:color="@color/divider" />
</shape>`,
  bg_card_accent: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#332A5E" />
    <corners android:radius="16dp" />
    <stroke android:width="1dp" android:color="#4B3C8F" />
</shape>`,
  bg_search: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface2" />
    <corners android:radius="12dp" />
</shape>`,
  bg_badge: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface3" />
    <corners android:radius="6dp" />
</shape>`,
  bg_art_placeholder: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface2" />
    <corners android:radius="12dp" />
</shape>`,
  bg_art_round: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface2" />
    <corners android:radius="18dp" />
</shape>`,
  bg_pill: `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="@color/surface2" />
    <corners android:radius="20dp" />
</shape>`,
};
for (const [name, xml] of Object.entries(backgrounds)) {
  fs.writeFileSync(path.join(dir, name + '.xml'), xml + '\n', 'utf8');
}
console.log('drawables générés:', Object.keys(icons).length + Object.keys(backgrounds).length);
