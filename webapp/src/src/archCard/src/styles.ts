import { css } from "lit";

import { global } from "../../lib/styles";

export default [
  global,
  css`
    :host {
      display: block;
      background-color: #fff;
      padding: 1rem 1rem 3rem 1rem;
      height: calc(100% - 4rem);
      box-shadow: 1px 1px 6px #888;
      font-size: 0.95rem;
      border-radius: 6px;
      position: relative;
    }

    :host .header {
      display: flex;
    }

    :host .header > *:first-child {
      flex-grow: 1;
    }

    :host .header > a {
      margin: auto;
    }

    :host .footer {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 1rem;
      text-align: center;
    }

    :host arch-tooltip {
      margin: auto 0 auto 0.5rem;
    }
  `,
];
