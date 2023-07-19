import{i as t,_ as a,a as e}from"./chunk-styles-75502ec5.js";import{A as s}from"./chunk-arch-data-table-815c3971.js";import{P as l}from"./chunk-arch-generate-dataset-form-6a3e9363.js";import{P as i}from"./chunk-helpers-94bb8932.js";import"./chunk-arch-loading-indicator-37c0007d.js";import"./chunk-arch-alert-a83c3a9d.js";import"./chunk-query-all-273a2103.js";import"./arch-sub-collection-builder-cd409a8e.js";var o=[t`
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
  `];let h=class extends s{willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===l.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["state!",l.NotStarted]],this.cellRenderers=[(t,a)=>a.state!==l.Finished?`${a.name}`:`<a href="${i.dataset(a.id,a.sample)}">\n               <span class="highlightable">${a.name}</span>\n            </a>`,void 0,(t,a)=>`<a href="${i.collection(a.collectionId)}">\n           <span class="highlightable">${t}</span>\n        </a>`,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,a)=>a.state===l.Running?"":null==t?void 0:t.slice(0,-3)],this.columnFilterDisplayMaps=[void 0,void 0,void 0,{100:"Yes",[-1]:"No"}],this.columns=["name","category","collectionName","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Collection","Sample","State","Started","Finished","Files"],this.filterableColumns=[!0,!0,!0,!0,!0,!1,!1,!1],this.searchColumns=["name","category","collectionName","state"],this.searchColumnLabels=["Name","Category","Collection","State"],this.singleName="Dataset",this.sort="-startTime",this.sortableColumns=[!0,!0,!0,!0,!0,!0,!0,!0],this.persistSearchStateInUrl=!0,this.pluralName="Datasets"}};h.styles=[...s.styles,...o],h=a([e("arch-dataset-explorer-table")],h);export{h as ArchDatasetExplorerTable};
//# sourceMappingURL=arch-dataset-explorer-table-f776706e.js.map
