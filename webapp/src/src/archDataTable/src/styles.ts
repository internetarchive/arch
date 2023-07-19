import { css } from "lit";

export default [
  css`
    data-table > .paginator-wrapper {
      margin: 0.5rem 0 0 0;
    }

    data-table > input.search {
      margin: 0.3rem 0;
    }

    data-table > div.action-button-wrapper > div.selection-buttons > button,
    data-table
      > div.action-button-wrapper
      > div.non-selection-buttons
      > button {
      font-size: 0.9rem;
    }

    data-table > table > thead > tr > th,
    data-table > table > tbody > tr > td {
      white-space: nowrap;
    }
  `,
];
