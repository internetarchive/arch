import{i as t,_ as a,a as e}from"./chunk-styles-48eb3c3c.js";import{t as s}from"./chunk-state-b70c226d.js";import{A as l}from"./chunk-arch-data-table-c93bbcda.js";import{P as i}from"./chunk-arch-generate-dataset-form-a4fc78d3.js";import{P as o}from"./chunk-helpers-7d3afcd3.js";import"./chunk-arch-loading-indicator-93cca752.js";import"./arch-hover-tooltip-826aa900.js";import"./chunk-scale-large-c2ff54fd.js";import"./chunk-query-all-ac9cba43.js";import"./chunk-arch-alert-84f9ff63.js";import"./arch-sub-collection-builder-b75d93c9.js";var r=[t`
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
  `];let c=class extends l{constructor(){super(...arguments),this.columnNameHeaderTooltipMap={category:"Dataset categories are Collection, Network, Text, and File Format",sample:"Sample datasets contain only the first 100 available records from a collection"}}willUpdate(t){super.willUpdate(t),this.apiCollectionEndpoint="/datasets",this.apiItemResponseIsArray=!0,this.apiItemTemplate="/datasets?collectionId=:collectionId&job=:jobId&sample=:isSample",this.itemPollPredicate=t=>t.state===i.Running,this.itemPollPeriodSeconds=3,this.apiStaticParamPairs=[["state!",i.NotStarted]],this.cellRenderers=[(t,a)=>a.state!==i.Finished?`${a.name}`:`<a href="${o.dataset(a.id,a.sample)}">\n               <span class="highlightable">${a.name}</span>\n            </a>`,void 0,(t,a)=>`<a href="${o.collection(a.collectionId)}">\n           <span class="highlightable">${t}</span>\n        </a>`,t=>-1===t?"No":"Yes",void 0,t=>null==t?void 0:t.slice(0,-3),(t,a)=>a.state===i.Running?"":null==t?void 0:t.slice(0,-3)],this.columnFilterDisplayMaps=[void 0,void 0,void 0,{100:"Yes",[-1]:"No"}],this.columns=["name","category","collectionName","sample","state","startTime","finishedTime","numFiles"],this.columnHeaders=["Dataset","Category","Collection","Sample","State","Started","Finished","Files"],this.filterableColumns=[!0,!0,!0,!0,!0,!1,!1,!1],this.searchColumns=["name","category","collectionName","state"],this.searchColumnLabels=["Name","Category","Collection","State"],this.singleName="Dataset",this.sort="-startTime",this.sortableColumns=[!0,!0,!0,!0,!0,!0,!0,!0],this.persistSearchStateInUrl=!0,this.pluralName="Datasets"}};c.styles=[...l.styles,...r],a([s()],c.prototype,"columnNameHeaderTooltipMap",void 0),c=a([e("arch-dataset-explorer-table")],c);export{c as ArchDatasetExplorerTable};
//# sourceMappingURL=arch-dataset-explorer-table-2f3815f3.js.map
