import{i as a,_ as l,e as s,s as n,y as t,a as e}from"./chunk-styles-4a7b21cd.js";var i=a`
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
`;let p=class extends n{constructor(){super(...arguments),this.text="Loading"}render(){const{text:a}=this;return t`
      ${a}
      <span class="la-ball-pulse">
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
      </div>
    `}};p.styles=i,l([s({type:String})],p.prototype,"text",void 0),p=l([e("arch-loading-indicator")],p);
//# sourceMappingURL=chunk-arch-loading-indicator-044bc257.js.map
