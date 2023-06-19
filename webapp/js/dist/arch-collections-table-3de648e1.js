import{i as t,_ as e,a}from"./chunk-styles-690ba0e4.js";import{A as l}from"./chunk-arch-data-table-06b40996.js";import{T as s}from"./chunk-pubsub-5f508603.js";import{P as i}from"./chunk-helpers-6405a525.js";import{a as o,i as n,h as r}from"./chunk-helpers-139f8162.js";import"./chunk-arch-loading-indicator-02b42d35.js";import"./arch-sub-collection-builder-2c0fad15.js";import"./chunk-arch-alert-46aea875.js";import"./arch-generate-dataset-form-e492768c.js";import"./chunk-types-df9f20ed.js";var c=[t`
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
  `];let h=class extends l{willUpdate(t){super.willUpdate(t),this.actionButtonLabels=["Generate Dataset","Create Custom Collection"],this.actionButtonSignals=[s.GENERATE_DATASET,s.CREATE_SUB_COLLECTION],this.apiCollectionEndpoint="/collections",this.cellRenderers=[(t,e)=>`\n        <a href="/collections/${o(e.id)}" title="${o(t)}">${t}</a>\n      `,{true:"Yes",false:"No"},(t,e)=>{if(null===t)return"";const a=e.lastJobId,l=e.lastJobSample?1:-1;return`\n          <a href="${i.dataset(`${e.id}:${a}`,l)}" title="${o(t)}">\n            ${t}\n          </a>\n        `},t=>t?n(t):"",(t,e)=>r(-1===e.sortSize?0:e.sortSize,1)],this.columns=["name","public","lastJobName","lastJobTime","sortSize"],this.columnHeaders=["Name","Public","Latest Dataset","Dataset Date","Size"],this.selectable=!0,this.singleName="Collection",this.pluralName="Collections"}postSelectionChangeHandler(t){const{dataTable:e}=this,{props:a}=e,l=t.length,s=1===l;a.actionButtonDisabled=[!s,!1],e.setSelectionActionButtonDisabledState(0===l)}selectionActionHandler(t,e){switch(t){case s.GENERATE_DATASET:window.location.href=i.generateCollectionDataset(e[0].id);break;case s.CREATE_SUB_COLLECTION:window.location.href=i.buildSubCollection(e.map((t=>t.id)))}}};h.styles=[...l.styles,...c],h=e([a("arch-collections-table")],h);export{h as ArchCollectionsTable};
//# sourceMappingURL=arch-collections-table-3de648e1.js.map
