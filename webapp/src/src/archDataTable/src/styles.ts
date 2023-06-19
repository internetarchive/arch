import { css } from "lit";

export default [
  css`
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
