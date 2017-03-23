import {Component, OnInit, AfterViewChecked, Input} from "@angular/core";
import {Observable}     from 'rxjs/Observable';

import {ServerCommunication} from "../server/app.server-communication";
import {Flag} from '../shared/flag';
import {configLoader} from '../core/app.config-loader.component';


@Component({
    selector: 'mainpage',
    template: `

<nav class="navbar navbar-default navbar-fixed-top">
  <div class="container-fluid">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#myNavbar">
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <a class="navbar-brand" href="#">BiBiGrid</a>
    </div>
    <div class="collapse navbar-collapse" id="myNavbar">
      <ul class="nav navbar-nav navbar-right">
        <li><a href="#home">HOME</a></li>
        <li><a href="#"><span class="glyphicon glyphicon-search"></span></a></li>
      </ul>
    </div>
  </div>
</nav>
   <div class="container-fluid text-center" style="width:80%">
<br>
<br>
<h1>BiBiGrid</h1> 
<h3><small>BiBiGrid is a tool for an easy cluster setup inside a cloud environment. It is written in Java and run on any OS a Java runtime is provided - any Java 8 is supported. 
BiBiGrid and its Cmdline UI based on a general cloud provider api. Currently there exists implementations for Amazon (AWS EC2 using the official AWS SDK) and OpenStack (Nova using OpenStack4J). 
BiBiGrid offers an easy configuration and maintenance of a started cluster via command-line.</small></h3>
</div>
<br>
<br>
<div class="container-fluid"> 
<table class="table table-inverse table-striped table-bordered table-hover">
<thead>
      <tr>
        <th>Short flag</th>
        <th>Long flag</th>
        <th>Description</th>
        <th>Input</th>
      </tr>
    </thead>
<tbody>
<tr *ngFor="let flag of flags" [attr.class]='flag.guiGroup'><th>{{flag.sFlag}}</th><th>{{flag.lFlag}}</th><th>{{flag.sDescription}}</th><th [ngSwitch]="flag.type"><div class="input-group">
                                                                                                                                        <input *ngSwitchCase="'string'" class="form-control" type="text" name="flag" id="{{flag.sFlag}}">
                                                                                                                                        <input *ngSwitchCase="'int'" class="form-control" type="number" name="flag" id="{{flag.sFlag}}">
                                                                                                                                        <input *ngSwitchCase="'float'" class="form-control" type="number" step="any" name="flag" id="{{flag.sFlag}}">
                                                                                                                                        <input *ngSwitchCase="'NONE'" class="form-control" type="checkbox" name="flag" id="{{flag.sFlag}}">
                                                                                                                                        <input *ngSwitchDefault class="form-control" type="text" name="flag" id="{{flag.sFlag}}">
                                                                                                                                        <span class="input-group-addon" id="addon-type">{{flag.type}}</span></div></th></tr>

</tbody>
</table>

<br>
<br>

<config-loader></config-loader>

<br>

<button class="btn btn-primary btn-lg btn-block" type="submit" (click)="sendFlags()" id="submit">
    <span class="glyphicon glyphicon-send" aria-hidden="true"></span> submit
</button>

<br>
<br>

 <div class="form-group">
  <label for="output"><h3>Output:</h3></label>
  <textarea class="form-control" rows="10" id="output" disabled="true">{{txtField}}</textarea>
</div>
</div>
`,
    providers: [ServerCommunication,configLoader]
})

export class mainPage implements OnInit,AfterViewChecked {

    @Input() userMode: string;
    errorMessage: string;
    flags: Flag[];
    mode = 'Observable';
    txtField: string = "";


    ngOnInit() {
        this.getFlags();
    }

    ngAfterViewChecked() {

        this.scrollDown();
        this.hideModeElem();
    }


    hideModeElem() {

        if (this.userMode === "intermediate" || this.userMode === "beginner") {

            let elements = (document.getElementsByClassName("expert"));

            for (let i = 0; i < elements.length; i++) {
                (<HTMLInputElement>elements[i]).style.display = 'none';
            }
        }

        if (this.userMode === "beginner") {

            let elements = (document.getElementsByClassName("intermediate"));

            for (let i = 0; i < elements.length; i++) {
                (<HTMLInputElement>elements[i]).style.display = 'none';
            }
        }
    }


    constructor(private server: ServerCommunication) {
    }

    readFields(): Flag[] {
        let tmp: Flag[] = [];
        for (let flag of this.flags) {
            if ((<HTMLInputElement>document.getElementById(flag.sFlag)).value != "") {
                switch ((<HTMLInputElement>document.getElementById(flag.sFlag)).type) {
                    case "text":
                        var fieldValue = (<HTMLInputElement>document.getElementById(flag.sFlag)).value;
                        tmp.push(new Flag(flag.sFlag, flag.lFlag, fieldValue, flag.type, flag.guiGroup));
                        break;
                    case "checkbox":
                        if ((<HTMLInputElement>document.getElementById(flag.sFlag)).checked) {
                            tmp.push(new Flag(flag.sFlag, flag.lFlag, "", flag.type, flag.guiGroup));
                        }
                        break;
                    default:
                        var fieldValue = (<HTMLInputElement>document.getElementById(flag.sFlag)).value;
                        tmp.push(new Flag(flag.sFlag, flag.lFlag, fieldValue, flag.type, flag.guiGroup));
                        break;
                }
            }
        }

        console.log(JSON.stringify(tmp));
        return tmp;
    }

    getFlags() {
        this.server.getFlags()
            .subscribe(
                flags => this.flags = flags,
                error => this.errorMessage = <any>error);
    }


    getAnswer() {
        this.server.getAnswer()
            .subscribe(
                answer => this.txtField += answer,
                error => this.errorMessage = <any>error);
        this.txtField += '\n';
        console.log(this.userMode);
    }

    sendFlags() {
        if (!this.readFields()) {
            return;
        }
        this.server.sendFlags(this.readFields())
            .subscribe(
                flags => console.log(flags),
                error => this.errorMessage = <any>error);
        this.getAnswer();
    }

    scrollDown() {
        var textArea = <HTMLInputElement>document.getElementById("output");
        textArea.scrollTop = textArea.scrollHeight;
    }


    createInputField(name: string, type: string): string {
        let field: string = "";

        switch (type) {
            case "string":
                field = '<input type="text" id="' + name + '">';
                break;
            case "int":
                field = '<input type="number" id="' + name + '">';
                break;
            case "boolean":
                field = '<input type="text" id="' + name + '">';
                break;
            case "float":
                field = '<input type="number" step="any" id="' + name + '">';
                break;
            case "NONE":
                field = '<input type="string" id="' + name + '">';
                break;
            default:
                field = '<input type="text" id="' + name + '">';
                break;
        }

        return field;
    }
}