import {Component} from "@angular/core";
import {ServerCommunication} from "../server/app.server-communication";
import {presetFlag} from '../shared/presetFlag';
import {configLink} from '../shared/configLink';

@Component({
    selector: 'config-loader',
    template: `

<button type="button" class="btn btn-primary btn-lg btn-block" data-toggle="modal" data-target="#configModal" (click)="getMasterConfig()">
  <span class="glyphicon glyphicon-cloud-download" aria-hidden="true"></span> load configuration
</button>

<div class="modal fade" id="configModal" tabindex="-1" role="dialog" aria-labelledby="configModalLabel">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id="configModalLabel">Load configuration</h4>
      </div>
        <div class="modal-body">
            <h2 align="center">Select configuration</h2>
            load predefined pipeline configurations from the internet
            <br>
            <div class="row">
                <div class="col-sm-6">
                <br>
                <select [(ngModel)]="selectedLinks">
                    <option *ngFor="let link of links" [ngValue]="link"> {{link.name}} </option>
                </select>
                </div>
                <div class="col-sm-6">
                <br>
                {{selectedLinks.description}}
                </div>
            </div>
        </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        <button type="button" class="btn btn-primary" data-dismiss="modal" data-toggle="modal" data-target="#clearModal">Load configuration</button>
      </div>
    </div>
  </div>
</div>


<button id="errorButton" [hidden]="true" data-toggle="modal" data-target="#errorModal">
  error modal
</button>

<div class="modal fade" id="errorModal" tabindex="-1" role="dialog" aria-labelledby="errorModalLabel">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id="errorModalLabel">Configuration invalid:</h4>
      </div>
        <div class="modal-body">
            Some flags defined by the configuration file you are loading, are missing in BiBiGrid.
            Either your configuration file is outdated/faulty or your BiBiGrid version is too old.
            Missing flags are skipped. The program may not function properly.            
        </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>


<button id="clearButton" [hidden]="true" data-toggle="modal" data-target="#clearModal">
  error modal
</button>

<div class="modal fade" id="clearModal" tabindex="-1" role="dialog" aria-labelledby="clearModalLabel">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title" id="clearModalLabel">Clear options:</h4>
      </div>
        <div class="modal-body">
            Do you want to clear all input fields before loading the configuration parameters?
            The configuration file will be loaded exactly as it is therefore your own inputs will be deleted.
        </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-primary" data-dismiss="modal" (click)="clearList()">Yes</button>
        <button type="button" class="btn btn-default" data-dismiss="modal" (click)="getConfig()">No</button>
      </div>
    </div>
  </div>
</div>

`,
    providers: []
})

export class configLoader {

    errorMessage: string;
    private masterConfigUrl: string = "https://raw.githubusercontent.com/bosterholz/bibigrid-gui/master/bibigrid-gui/src/main/resources/public/app/shared/masterConfig.json";
    links = [{'name': '', 'description': '', 'link': ""}];
    selectedLinks = this.links[0];

    constructor(private server: ServerCommunication) {
    }

    getMasterConfig() {

        this.server.getConfLinks(this.masterConfigUrl)
            .subscribe(
                conf => this.listFoundConfigs(conf),
                error => this.errorMessage = <any>error);
    }

    getConfig() {

        this.server.getConfig(this.selectedLinks.link)
            .subscribe(
                conf => this.setConfig(conf),
                error => this.errorMessage = <any>error);
    }

    listFoundConfigs(configLinks: configLink[]) {
        this.links = configLinks;
    }

    clearList() {

        let elements = (document.getElementsByName("flag"));

        for (let i = 0; i < elements.length; i++) {

            switch ((<HTMLInputElement>elements[i]).type) {
                case "checkbox":
                    (<HTMLInputElement>elements[i]).checked = false;
                    break;
                default:
                    (<HTMLInputElement>elements[i]).value = "";
                    break;
            }
        }

        this.getConfig();
    }

    setConfig(config: presetFlag[]) {
        let error: boolean = false;

        for (let flag of config) {
            if ((<HTMLInputElement>document.getElementById(flag.sFlag))) {
                switch ((<HTMLInputElement>document.getElementById(flag.sFlag)).type) {
                    case "checkbox":
                        (<HTMLInputElement>document.getElementById(flag.sFlag)).checked = (flag.value == "true");
                        break;
                    default:
                        (<HTMLInputElement>document.getElementById(flag.sFlag)).value = flag.value;
                        break;
                }
            } else {
                error = true;
            }
        }

        if (error) {
            (<HTMLInputElement>document.getElementById("errorButton")).click();
        }
    }
}