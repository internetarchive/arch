import { css } from "lit";

/*
 * Load Awesome v1.1.0 (http://github.danielcardoso.net/load-awesome/)
 * Copyright 2015 Daniel Cardoso <@DanielCardoso>
 * Licensed under MIT
 */

export default css`
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
`;
