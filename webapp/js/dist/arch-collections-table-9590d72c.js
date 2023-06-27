import{i as t,_ as e,a}from"./chunk-styles-75502ec5.js";import{A as l}from"./chunk-arch-data-table-6bbf8534.js";import{T as s}from"./chunk-pubsub-5f508603.js";import{P as i}from"./chunk-helpers-31bf771e.js";import{a as o,i as n,h as c}from"./chunk-helpers-139f8162.js";import"./chunk-arch-loading-indicator-37c0007d.js";import"./arch-sub-collection-builder-5c9f5725.js";import"./chunk-arch-alert-384569c4.js";import"./chunk-arch-generate-dataset-form-abec12ec.js";import"./chunk-query-all-273a2103.js";var r=[t`
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
  `];let h=class extends l{willUpdate(t){super.willUpdate(t),this.actionButtonLabels=["Generate Dataset","Create Custom Collection"],this.actionButtonSignals=[s.GENERATE_DATASET,s.CREATE_SUB_COLLECTION],this.apiCollectionEndpoint="/collections",this.cellRenderers=[(t,e)=>`\n        <a href="/collections/${o(e.id)}" title="${o(t)}">${t}</a>\n      `,{true:"Yes",false:"No"},(t,e)=>{if(null===t)return"";const a=e.lastJobId,l=e.lastJobSample?1:-1;return`\n          <a href="${i.dataset(`${e.id}:${a}`,l)}" title="${o(t)}">\n            ${t}\n          </a>\n        `},t=>t?n(t):"",(t,e)=>c(-1===e.sortSize?0:e.sortSize,1)],this.columns=["name","public","lastJobName","lastJobTime","sortSize"],this.columnHeaders=["Name","Public","Latest Dataset","Dataset Date","Size"],this.selectable=!0,this.singleName="Collection",this.pluralName="Collections"}postSelectionChangeHandler(t){const{dataTable:e}=this,{props:a}=e,l=t.length,s=1===l;a.actionButtonDisabled=[!s,!1],e.setSelectionActionButtonDisabledState(0===l)}selectionActionHandler(t,e){switch(t){case s.GENERATE_DATASET:window.location.href=i.generateCollectionDataset(e[0].id);break;case s.CREATE_SUB_COLLECTION:window.location.href=i.buildSubCollection(e.map((t=>t.id)))}}};h.styles=[...l.styles,...r],h=e([a("arch-collections-table")],h);export{h as ArchCollectionsTable};
//# sourceMappingURL=arch-collections-table-9590d72c.js.map
