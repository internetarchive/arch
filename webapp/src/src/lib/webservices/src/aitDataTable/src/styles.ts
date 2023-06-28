import { css } from "lit";

import GlobalStyles from "../../lib/styles.js";

export default [
  GlobalStyles,
  css`
    data-table {
      display: block;
      font-size: 0.875rem;
    }

    data-table > table {
      border-collapse: collapse;
      width: 100%;
    }

    data-table > table > tbody > tr {
      border-bottom: 1px solid #e1e1e1;
    }

    data-table > table > tbody > tr.selected {
      box-shadow: inset 0 -1px 0 2px #888;
    }

    data-table > table > tbody > tr:hover {
      background-color: rgba(0, 0, 0, 0.04);
    }

    data-table > table > thead > tr > th,
    data-table > table > tbody > tr > td {
      padding: 0.5rem;
      text-align: left;
    }

    data-table > table > thead > tr > th:last-child,
    data-table > table > tbody > tr > td:last-child {
      padding-right: 0.75rem;
    }

    data-table > table > thead > tr > th {
      background-color: rgb(42, 40, 44);
      color: #fff;
      font-weight: normal;
      padding: 0.7rem inherit;
    }

    data-table > table > tbody > tr > td {
      max-width: 0;
      overflow-x: hidden;
      text-overflow: ellipsis;
      color: rgb(106, 109, 112);
    }

    data-table > table > tbody > tr > td * {
      overflow-x: inherit;
      text-overflow: inherit;
    }

    data-table > table > thead > tr > th.select,
    data-table > table > tbody > tr > td.select {
      width: 2rem;
    }

    data-table > table > tbody > tr > td.name > span.COLLECTION:before {
      content: "\\25EC";
      margin-right: 1rem;
      display: inline-block;
      transform: rotate(0.5turn);
      vertical-align: -0.1em;
    }

    data-table > table > tbody > tr > td.name > span.FOLDER:before {
      content: "\\1f4c1";
      margin-right: 1rem;
      vertical-align: 0.15em;
    }

    data-table > table > tbody > tr > td.name > span.FILE:before {
      content: "\\1f4c4";
      margin-right: 1rem;
    }

    data-table > table > tbody > tr.clickable {
      cursor: pointer;
    }

    data-table > table > tbody > tr.unselectable {
      cursor: not-allowed;
    }

    data-table > table > tbody > tr.clickable > td a:hover {
      text-decoration: underline;
    }

    data-table > table > thead > tr > th > button {
      border: none;
      color: inherit;
      font-size: inherit;
      font-weight: inherit;
    }

    data-table > table > thead > tr > th > button,
    data-table-filter-popover > button.filter {
      margin: 0;
      padding: 0;
      background-color: transparent;
      font-size: inherit;
      cursor: pointer;
      color: inherit;
      border: none;
    }

    data-table-filter-popover > button.filter {
      margin-left: 0.25rem;
      font-size: 0.75rem;
    }

    data-table-filter-popover > button.filter:before {
      content: "\\1F50E";
      display: inline;
      transform: rotate(0.5turn);
    }

    data-table-filter-popover > button.filter > span.applied-count {
      vertical-align: super !important;
    }

    data-table-select-all-checkbox .popover-content {
      color: #000;
    }

    data-table-select-all-checkbox ui5-popover.loading {
      color: #fff;
      padding: 0 1rem;
      font-size: 0.9rem;
      background: #888;
    }

    data-table-select-all-checkbox ol,
    data-table-filter-popover ol {
      margin: 0;
      padding-inline: 0;
      list-style: none;
      color: #555;
    }

    data-table-select-all-checkbox ol input[type="checkbox"],
    data-table-filter-popover ol input[type="checkbox"] {
      margin-bottom: 0;
    }

    data-table-select-all-checkbox ol > li,
    data-table-filter-popover ol > li > * {
      font-size: 0.9rem;
      padding: 0.5em 1.25em;
      cursor: pointer;
    }

    data-table-filter-popover ol > li > label {
      display: block;
    }

    data-table-select-all-checkbox ol > li:hover,
    data-table-filter-popover ol > li > label:hover {
      color: #000;
      background-color: rgba(0, 0, 0, 0.1);
    }

    data-table-select-all-checkbox ol > li.nothing-to-do:hover {
      color: inherit;
      background-color: transparent;
      cursor: not-allowed;
    }

    data-table-filter-popover button.clear {
      background: none;
      color: #555;
      margin: 0;
      padding: 0.5em 1.25em;
      font-size: 0.9rem;
      border: none;
      cursor: pointer;
      width: 100%;
      text-align: left;
    }

    data-table-filter-popover button.clear:hover {
      background-color: rgba(0, 0, 0, 0.1);
    }

    data-table > table > thead > tr > th > button.sort-desc:after,
    data-table > table > thead > tr > th > button.sort-asc:after {
      margin-left: 0.2em;
      font-size: 0.8em;
    }

    data-table > table > thead > tr > th > button.sort-asc:after {
      content: "\\25B2";
    }

    data-table > table > thead > tr > th > button.sort-desc:after {
      content: "\\25BC";
    }

    data-table > table > thead > tr > th > button:hover {
      color: #ccc;
      background-color: transparent;
    }

    data-table input[type="checkbox"] {
      cursor: pointer;
      width: 1rem;
      height: 1rem;
    }

    data-table > table > tbody > tr > td > span.null-placeholder {
      opacity: 0.5;
    }

    data-table > table > tbody > tr > td.loading {
      font-style: italic;
    }

    data-table > table > tbody > tr > td.no-results {
      font-weight: bold;
    }

    data-table > table > tbody > tr > td span.match-highlight {
      font-weight: bold;
    }

    data-table input.search {
      display: inline-block;
      width: 50%;
      margin-left: 0.5rem;
      font-size: inherit;
      padding: 0.25rem;
    }

    data-table button.clear-input {
      display: inline-block;
      background: none;
      border: none;
      color: rgb(100, 100, 100);
      font-size: 1rem;
      cursor: pointer;
    }
    data-table button.clear-input:hover {
      color: #969696;
    }
    data-table button.clear-input:active {
      color: #767676;
    }

    data-table a.download {
      float: right;
      margin-top: 1rem;
      font-size: 0.8rem;
    }

    data-table .action-button-wrapper {
      display: table;
      width: 100%;
      font-size: 0;
      padding: 0.5rem 0;
    }

    data-table .action-button-wrapper button {
      background-color: var(
        --data-table-action-button-background-color,
        initial
      );
      border: var(--data-table-action-button-border, none);
      color: var(--data-table-action-button-color, initial);
      cursor: var(--data-table-action-button-cursor, pointer);
      font-size: var(--data-table-action-button-font-size, 0.8125rem);
      margin-bottom: 0;
      padding: var(--data-table-action-button-padding, 0.4rem 1rem);
      transition: var(--data-table-action-button-transition, pointer);
      white-space: nowrap;
    }

    data-table .action-button-wrapper button:hover {
      background-color: var(
        --data-table-action-button-hover-background-color,
        initial
      );
      color: var(--data-table-action-button-hover-color, initial);
    }

    data-table .action-button-wrapper button:first-child {
      border-top-left-radius: 3px;
      border-bottom-left-radius: 3px;
    }

    data-table .action-button-wrapper button:last-child {
      border-top-right-radius: 3px;
      border-bottom-right-radius: 3px;
    }

    data-table .action-button-wrapper > .selection-buttons,
    data-table .action-button-wrapper > .non-selection-buttons {
      display: table-cell;
      white-space: nowrap;
    }

    data-table .action-button-wrapper > .selection-buttons {
      padding-right: 0.3rem;
    }

    data-table .action-button-wrapper > .selection-buttons button {
      margin-left: 0.25rem;
    }

    data-table .action-button-wrapper > .selection-buttons button:first-child {
      margin-left: 0;
    }

    data-table .action-button-wrapper > .selection-buttons button:disabled,
    data-table
      .action-button-wrapper
      > .selection-buttons
      button:disabled:hover {
      background-color: #ddd;
      color: #444;
      cursor: not-allowed;
    }

    data-table .action-button-wrapper > .non-selection-buttons {
      text-align: right;
    }

    data-table .paginator-wrapper {
      height: 1.6rem;
      text-align: right;
      font-size: var(--data-table-paginator-wrapper-font-size, 0.9rem);
      padding-bottom: 0.3rem;
      margin: 0.5rem 0.5rem 0 0.5rem;
    }

    data-table .paginator-wrapper > span.showing {
      display: table-cell;
      white-space: nowrap;
      color: #666;
    }

    data-table .paginator-wrapper > paginator-control {
      display: table-cell;
      width: 100%;
    }

    data-table paginator-control button {
      background-color: var(
        --data-table-paginator-control-button-background-color,
        transparent
      );
      border: var(--data-table-paginator-control-button-border, none);
      color: var(--data-table-paginator-control-button-color, inherit);
      cursor: pointer;
      font-size: var(--data-table-paginator-control-button-font-size, inherit);
      padding: var(--data-table-paginator-control-button-padding, 0);
    }

    data-table paginator-control button:disabled {
      color: #000;
      cursor: not-allowed;
      opacity: 0.3;
    }

    data-table paginator-control button:hover {
      color: #222;
    }

    data-table paginator-control button:disabled:hover {
      color: var(--data-table-paginator-control-button-color, inherit);
    }

    data-table paginator-control button {
      margin: 0 0.2rem;
    }

    data-table paginator-control button:first-child,
    data-table paginator-control button:last-child {
      margin: 0;
    }
  `,
];
