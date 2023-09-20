import { css } from "lit";

import { global } from "../../lib/styles";

export default [
  global,
  css`
    sp-popover {
      padding: 1rem;
    }

    div[role="dialog"] > h2 {
      font-size: 1rem;
      border-bottom: solid 1px #888;
      padding-bottom: 0.5rem;
      margin-top: 0;
    }

    div[role="dialog"] > h2 > span {
      display: inline-block;
      width: 1.4rem;
    }

    div[role="dialog"] > div.text {
      margin-left: 1.4rem;
      font-size: 0.9rem;
      line-height: 1.2em;
      color: #444;
    }

    div[role="dialog"] > div.learn-more {
      text-align: right;
      padding-top: 1rem;
    }
  `,
];
