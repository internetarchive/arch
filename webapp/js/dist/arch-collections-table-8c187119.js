import{i as t,_ as e,a}from"./chunk-styles-ad31501a.js";import{A as s}from"./chunk-arch-data-table-c422227c.js";import{T as l}from"./chunk-pubsub-5f508603.js";import{P as i}from"./chunk-helpers-e724cca8.js";import{h as o,i as n,a as c}from"./chunk-arch-loading-indicator-2842cdb6.js";import"./chunk-state-1d3d2492.js";import"./arch-hover-tooltip-36cde9ca.js";import"./chunk-scale-large-891207ee.js";import"./arch-sub-collection-builder-46e3dce6.js";import"./chunk-arch-alert-ef577355.js";import"./chunk-arch-generate-dataset-form-22ef0d7d.js";import"./chunk-query-all-dcc1a25c.js";var r=[t`
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
  `];let h=class extends s{willUpdate(t){super.willUpdate(t),this.actionButtonLabels=["Generate Dataset","Create Custom Collection"],this.actionButtonSignals=[l.GENERATE_DATASET,l.CREATE_SUB_COLLECTION],this.apiCollectionEndpoint="/collections",this.cellRenderers=[(t,e)=>`\n        <a href="/collections/${o(e.id)}" title="${o(t)}">\n          <span class="highlightable">${t}</span>\n        </a>\n      `,{true:"Yes",false:"No"},(t,e)=>{if(null===t)return"";const a=e.lastJobId,s=e.lastJobSample?1:-1;return`\n          <a href="${i.dataset(`${e.id}:${a}`,s)}" title="${o(t)}">\n            <span class="highlightable">${t}</span>\n          </a>\n        `},t=>t?n(t):"",(t,e)=>c(-1===e.sortSize?0:e.sortSize,1)],this.columnFilterDisplayMaps=[void 0,{true:"Yes",false:"No"}],this.columns=["name","public","lastJobName","lastJobTime","sortSize"],this.columnHeaders=["Name","Public","Latest Dataset","Dataset Date","Size"],this.selectable=!0,this.sort="name",this.sortableColumns=[!0,!1,!0,!0,!0],this.filterableColumns=[!1,!0],this.searchColumns=["name"],this.searchColumnLabels=["Name"],this.singleName="Collection",this.persistSearchStateInUrl=!0,this.pluralName="Collections"}postSelectionChangeHandler(t){const{dataTable:e}=this,{props:a}=e,s=t.length,l=1===s;a.actionButtonDisabled=[!l,!1],e.setSelectionActionButtonDisabledState(0===s)}selectionActionHandler(t,e){switch(t){case l.GENERATE_DATASET:window.location.href=i.generateCollectionDataset(e[0].id);break;case l.CREATE_SUB_COLLECTION:window.location.href=i.buildSubCollection(e.map((t=>t.id)))}}};h.styles=[...s.styles,...r],h=e([a("arch-collections-table")],h);export{h as ArchCollectionsTable};
//# sourceMappingURL=arch-collections-table-8c187119.js.map
