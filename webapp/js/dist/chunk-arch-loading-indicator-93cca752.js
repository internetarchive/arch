import{i as a,_ as n,e as t,b as e,y as i,a as l}from"./chunk-styles-48eb3c3c.js";function s(a){return a.replace(/"/g,"&quot;")}function o(a,n=3){for(const t of["","Ki","Mi","Gi","Ti","Pi"]){if(a<1024){const[e,i]=a.toString().split(".");return`${e}${i?`.${i.slice(0,n)}`:""} ${t}B`}a/=1024}return""}function r(a,n=!1){const t={month:"short",day:"numeric",year:"numeric"};return n?new Date(a).toLocaleTimeString(navigator.language,{...t,hour:"numeric",minute:"2-digit",timeZoneName:"short"}):new Date(a).toLocaleDateString(navigator.language,t)}var c=a`
  :host {
    font-style: italic;
    color: var(--color, #666);
  }

  .la-ball-pulse {
    display: inline-block;
  }

  .la-ball-pulse > span {
    display: inline-block;
    font-size: 0.2em;
    animation: ball-pulse 1s ease infinite;
  }

  .la-ball-pulse > span:nth-child(1) {
    animation-delay: -200ms;
  }

  .la-ball-pulse > span:nth-child(2) {
    animation-delay: -100ms;
  }

  .la-ball-pulse > span:nth-child(3) {
    animation-delay: 0ms;
  }

  @keyframes ball-pulse {
    0%,
    60%,
    100% {
      opacity: 1;
    }
    30% {
      opacity: 0.1;
    }
  }
`;let p=class extends e{constructor(){super(...arguments),this.text="Loading"}render(){const{text:a}=this;return i`
      ${a}
      <span class="la-ball-pulse">
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
      </div>
    `}};p.styles=c,n([t({type:String})],p.prototype,"text",void 0),p=n([l("arch-loading-indicator")],p);export{o as a,s as h,r as i};
//# sourceMappingURL=chunk-arch-loading-indicator-93cca752.js.map
