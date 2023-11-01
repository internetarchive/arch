import{i as t,_ as e,a}from"./chunk-styles-bfd25595.js";import{t as s}from"./chunk-state-97ec0d61.js";import{A as l}from"./chunk-arch-data-table-093b505d.js";import{P as i}from"./chunk-arch-generate-dataset-form-da670fee.js";import{P as o}from"./chunk-helpers-ecdb04c9.js";import"./chunk-arch-loading-indicator-418d50f9.js";import"./arch-hover-tooltip-24269d55.js";import"./chunk-scale-large-1a48103b.js";import"./chunk-query-all-fe6f60e4.js";import"./chunk-arch-alert-77120ba7.js";import"./arch-sub-collection-builder-ce5e481e.js";var r=[t`
    data-table {
      min-width: 60rem;
    }

    data-table > table {
      table-layout: fixed;
    }

    data-table > table > thead > tr > th.category {
      width: 8em;
    }

    data-table > table > thead > tr > th.sample {
      width: 7em;
    }

    data-table > table > thead > tr > th.state {
      width: 6em;
    }

    data-table > table > thead > tr > th.started {
      width: 9em;
    }

    data-table > table > thead > tr > th.finished {
      width: 9em;
    }

    data-table > table > thead > tr > th.files {
      width: 3em;
    }

    data-table > table > thead > tr > th {
      max-width: none;
    }
  `];let d=class extends l{constructor(){super(...arguments),this.columnNameHeaderTooltipMap={category:"Dataset categories are Collection, Network, Text, and File Format",sample:"Sample datasets contain only the first 100 available records from a collection"}}willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===i.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["state!",i.NotStarted]],this.cellRenderers=[(t,e)=>e.state!==i.Finished?`${e.name}`:`<a href="${o.dataset(e.id,e.sample)}">\n               <span class="highlightable">${e.name}</span>\n            </a>`,void 0,(t,e)=>`<a href="${o.collection(e.collectionId)}">\n           <span class="highlightable">${t}</span>\n        </a>`,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,e)=>e.state===i.Running?"":null==t?void 0:t.slice(0,-3)],this.columnFilterDisplayMaps=[void 0,void 0,void 0,{100:"Yes",[-1]:"No"}],this.columns=["name","category","collectionName","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Collection","Sample","State","Started","Finished","Files"],this.filterableColumns=[!0,!0,!0,!0,!0,!1,!1,!1],this.searchColumns=["name","category","collectionName","state"],this.searchColumnLabels=["Name","Category","Collection","State"],this.singleName="Dataset",this.sort="-startTime",this.sortableColumns=[!0,!0,!0,!0,!0,!0,!0,!0],this.persistSearchStateInUrl=!0,this.pluralName="Datasets"}};d.styles=[...l.styles,...r],e([s()],d.prototype,"columnNameHeaderTooltipMap",void 0),d=e([a("arch-dataset-explorer-table")],d);export{d as ArchDatasetExplorerTable};
//# sourceMappingURL=arch-dataset-explorer-table-8b15604e.js.map
