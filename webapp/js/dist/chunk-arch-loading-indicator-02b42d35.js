import{i as a,_ as l,s,y as n,a as i}from"./chunk-styles-690ba0e4.js";var e=a`
  :host {
    font-style: italic;
    color: #666;
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
`;let p=class extends s{render(){return n`
      Loading
      <span class="la-ball-pulse">
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
        <span>&#x2B24;</span>
      </div>
    `}};p.styles=e,p=l([i("arch-loading-indicator")],p);
//# sourceMappingURL=chunk-arch-loading-indicator-02b42d35.js.map
