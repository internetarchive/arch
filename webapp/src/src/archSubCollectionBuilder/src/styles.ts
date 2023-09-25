import { css } from "lit";

import { global } from "../../lib/styles";

export default [
  global,
  css`
    label {
      margin-top: 1rem;
    }

    label:first-of-type {
      margin-top: 0;
    }

    em {
      line-height: 1.2em;
    }

    button[type="submit"] {
      font-size: 1rem;
      padding: 0.8rem 1.2rem;
      margin-top: 1rem;
    }

    select#sources,
    input#name,
    input#surts {
      width: 100%;
    }

    select#sources {
      resize: vertical;
    }

    input#status,
    input#mime {
      width: 50%;
    }
  `,
];
