import{i as t,_ as a,a as e}from"./chunk-styles-48eb3c3c.js";import{A as s}from"./chunk-arch-data-table-c93bbcda.js";import{T as l}from"./chunk-pubsub-5f508603.js";import{P as i}from"./chunk-helpers-7d3afcd3.js";import{h as o,i as n,a as c}from"./chunk-arch-loading-indicator-93cca752.js";import"./chunk-state-b70c226d.js";import"./arch-hover-tooltip-826aa900.js";import"./chunk-scale-large-c2ff54fd.js";import"./arch-sub-collection-builder-b75d93c9.js";import"./chunk-arch-alert-84f9ff63.js";import"./chunk-arch-generate-dataset-form-a4fc78d3.js";import"./chunk-query-all-ac9cba43.js";var r=[t`
    data-table > table {
      table-layout: fixed;
    }

    data-table > table > thead > tr > th.public {
      width: 5em;
    }

    data-table > table > thead > tr > th.dataset-date {
      width: 7em;
    }

    data-table > table > thead > tr > th.size {
      width: 7em;
    }

    data-table > table > thead > tr > th {
      max-width: none;
    }
  `];let h=class extends s{willUpdate(t){super.willUpdate(t),this.actionButtonLabels=["Generate Dataset","Create Custom Collection"],this.actionButtonSignals=[l.GENERATE_DATASET,l.CREATE_SUB_COLLECTION],this.apiCollectionEndpoint="/collections",this.cellRenderers=[(t,a)=>`\n        <a href="/collections/${o(a.id)}" title="${o(t)}">\n          <span class="highlightable">${t}</span>\n        </a>\n      `,{true:"Yes",false:"No"},(t,a)=>{if(null===t)return"";const e=a.lastJobId,s=a.lastJobSample?1:-1;return`\n          <a href="${i.dataset(`${a.id}:${e}`,s)}" title="${o(t)}">\n            <span class="highlightable">${t}</span>\n          </a>\n        `},t=>t?n(t):"",(t,a)=>c(-1===a.sortSize?0:a.sortSize,1)],this.columnFilterDisplayMaps=[void 0,{true:"Yes",false:"No"}],this.columns=["name","public","lastJobName","lastJobTime","sortSize"],this.columnHeaders=["Name","Public","Latest Dataset","Dataset Date","Size"],this.selectable=!0,this.sort="name",this.sortableColumns=[!0,!1,!0,!0,!0],this.filterableColumns=[!1,!0],this.searchColumns=["name"],this.searchColumnLabels=["Name"],this.singleName="Collection",this.persistSearchStateInUrl=!0,this.pluralName="Collections"}postSelectionChangeHandler(t){const{dataTable:a}=this,{props:e}=a,s=t.length,l=1===s;e.actionButtonDisabled=[!l,!1],a.setSelectionActionButtonDisabledState(0===s)}selectionActionHandler(t,a){switch(t){case l.GENERATE_DATASET:window.location.href=i.generateCollectionDataset(a[0].id);break;case l.CREATE_SUB_COLLECTION:window.location.href=i.buildSubCollection(a.map((t=>t.id)))}}};h.styles=[...s.styles,...r],h=a([e("arch-collections-table")],h);export{h as ArchCollectionsTable};
//# sourceMappingURL=arch-collections-table-340db407.js.map
