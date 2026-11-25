import { Component, computed, input } from '@angular/core';

type Theme = 'mumbai' | 'goa' | 'jaipur' | 'bengaluru' | 'manali' | 'udaipur' | 'kochi' | 'rishikesh' | 'generic';

interface Palette {
  sky: [string, string, string];
  sun: string;
  sx: number;
  sy: number;
  sr: number;
}

const PALETTES: Record<Theme, Palette> = {
  mumbai: { sky: ['#1d2260', '#7a3b8f', '#f58a5a'], sun: '#ffd27a', sx: 290, sy: 152, sr: 26 },
  goa: { sky: ['#ff9a62', '#ffc58a', '#ffe6b3'], sun: '#fff3c4', sx: 140, sy: 142, sr: 30 },
  jaipur: { sky: ['#f6a6a0', '#f9cfa3', '#fde9bd'], sun: '#fff0c9', sx: 322, sy: 70, sr: 22 },
  bengaluru: { sky: ['#7fd1c6', '#bfe9d7', '#eaf7e1'], sun: '#fff6c8', sx: 70, sy: 66, sr: 22 },
  manali: { sky: ['#8fb4ee', '#c9dcf7', '#eef4fd'], sun: '#ffffff', sx: 330, sy: 56, sr: 16 },
  udaipur: { sky: ['#3a2d6e', '#c9638a', '#ffb36b'], sun: '#ffe0a3', sx: 92, sy: 146, sr: 24 },
  kochi: { sky: ['#2b8f98', '#8ed0b6', '#ffe9a8'], sun: '#fff1b8', sx: 330, sy: 118, sr: 24 },
  rishikesh: { sky: ['#9dd7e6', '#cfeadb', '#f4f7d9'], sun: '#fff7cc', sx: 66, sy: 76, sr: 20 },
  generic: { sky: ['#5b6bd6', '#b3a2e6', '#ffd9b0'], sun: '#fff0c9', sx: 300, sy: 110, sr: 24 },
};

const KEYWORDS: [string, Theme][] = [
  ['mumbai', 'mumbai'],
  ['goa', 'goa'],
  ['calangute', 'goa'],
  ['jaipur', 'jaipur'],
  ['bengaluru', 'bengaluru'],
  ['bangalore', 'bengaluru'],
  ['manali', 'manali'],
  ['udaipur', 'udaipur'],
  ['kochi', 'kochi'],
  ['cochin', 'kochi'],
  ['rishikesh', 'rishikesh'],
];

let nextUid = 0;

/**
 * A small hand-built SVG landscape per destination, so every hotel gets its own picture
 * without depending on any external image host.
 */
@Component({
  selector: 'app-hotel-scene',
  templateUrl: './hotel-scene.html',
  styleUrl: './hotel-scene.css',
})
export class HotelScene {
  readonly location = input.required<string>();
  /** Plays the one-time sunrise and slow cloud drift (used on large hero scenes). */
  readonly animated = input(false);

  protected readonly uid = `sc${nextUid++}`;
  protected readonly theme = computed<Theme>(() => {
    const text = this.location().toLowerCase();
    return KEYWORDS.find(([key]) => text.includes(key))?.[1] ?? 'generic';
  });
  protected readonly palette = computed(() => PALETTES[this.theme()]);
}
