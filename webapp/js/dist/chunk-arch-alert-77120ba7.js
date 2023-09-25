import{d as t,x as e,g as s,B as r,i,_ as a,e as n,b as o,y as l,a as d}from"./chunk-styles-bfd25595.js";class c{static async get(t,e=[]){const s=new URLSearchParams(e.map((([t,e,s])=>[`${String(t)}${"!="===e?"!":""}`,String(s)])));return(await fetch(`${c.BasePath}${t}${e?`?${s.toString()}`:""}`)).json()}static get collections(){return{get:(t=[])=>c.get("/collections",t)}}static get datasets(){return{get:(t=[])=>c.get("/datasets",t)}}}c.BasePath="/api";const h="-SAMPLE",u=/^[1-5]\d\d$/,g="[a-zA-Z0-9\\-]",p=new RegExp(`^${g}+,${g}+(${g}+,?)*((\\))|(\\)/.*))?$`),$=2;class y{constructor(t){}get _$AU(){return this._$AM._$AU}_$AT(t,e,s){this._$Ct=t,this._$AM=e,this._$Ci=s}_$AS(t,e){return this.update(t,e)}update(t,e){return this.render(...e)}}
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */class m extends y{constructor(e){if(super(e),this.it=t,e.type!==$)throw Error(this.constructor.directiveName+"() can only be used in child bindings")}render(s){if(s===t||null==s)return this._t=void 0,this.it=s;if(s===e)return s;if("string"!=typeof s)throw Error(this.constructor.directiveName+"() called with a non-string value");if(s===this.it)return this._t;this.it=s;const r=[s];return r.raw=r,this._t={_$litType$:this.constructor.resultType,strings:r,values:[]}}}m.directiveName="unsafeHTML",m.resultType=1;const f=(t=>(...e)=>({_$litDirective$:t,values:e}))(m);var v,b=[s,r,i`
    div.alert {
      display: flex;
      padding: 0;
    }

    p {
      font-size: 1rem;
      line-height: 1.6rem;
      flex-grow: 1;
      margin: 0;
      padding: 1.2rem 0 1.2rem 1.2rem;
    }

    button {
      align-self: flex-start;
      padding: 1.2rem;
      font-size: 1.2rem;
    }

    button:hover {
      font-weight: bold;
    }
  `];!function(t){t.Danger="danger",t.Dark="dark",t.Info="info",t.Light="light",t.Primary="primary",t.Secondary="secondary",t.Success="success",t.Warning="warning"}(v||(v={}));let _=class extends o{constructor(){super(...arguments),this.alertClass=v.Primary,this.hidden=!1,this.message=""}render(){return l`
      <div
        class="alert alert-${this.alertClass}"
        style="display: ${this.hidden?"none":"flex"}"
        role="alert"
      >
        <p>${f(this.message)}</p>
        <button
          type="button"
          class="close"
          data-dismiss="alert"
          aria-label="Close"
          style="background-color: transparent;"
          @click=${this.hide}
        >
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
    `}hide(){this.setAttribute("hidden","")}show(){this.removeAttribute("hidden")}};_.styles=b,a([n({type:String})],_.prototype,"alertClass",void 0),a([n({type:Boolean})],_.prototype,"hidden",void 0),a([n({type:String})],_.prototype,"message",void 0),_=a([d("arch-alert")],_);export{v as A,u as H,h as S,c as a,p as b};
//# sourceMappingURL=chunk-arch-alert-77120ba7.js.map
