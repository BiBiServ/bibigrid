"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var core_1 = require("@angular/core");
var app_server_communication_1 = require("./server/app.server-communication");
var app_bibigrid_gui_navbar_1 = require("./core/app.bibigrid-gui.navbar");
var flag_1 = require("./shared/flag");
var BiBiGridGui = (function () {
    function BiBiGridGui(server) {
        this.server = server;
        this.mode = 'Observable';
        this.txtField = "";
        this.testVar = "text";
        this.userSet = false;
    }
    BiBiGridGui.prototype.ngOnInit = function () {
        this.getFlags();
    };
    BiBiGridGui.prototype.ngAfterViewChecked = function () {
        this.scrollDown();
    };
    BiBiGridGui.prototype.setUser = function (message) {
        alert(message);
        this.userSet = message;
    };
    BiBiGridGui.prototype.readFields = function () {
        var tmp = [];
        for (var _i = 0, _a = this.flags; _i < _a.length; _i++) {
            var flag = _a[_i];
            if (document.getElementById(flag.sFlag).value != "") {
                var fieldValue = document.getElementById(flag.sFlag).value;
                tmp.push(new flag_1.Flag(flag.sFlag, flag.lFlag, fieldValue, flag.type));
            }
        }
        // tmp.push(new Flag("h", "help", "", "NONE"));
        console.log(JSON.stringify(tmp));
        return tmp;
    };
    BiBiGridGui.prototype.getFlags = function () {
        var _this = this;
        this.server.getFlags()
            .subscribe(function (flags) { return _this.flags = flags; }, function (error) { return _this.errorMessage = error; });
    };
    BiBiGridGui.prototype.getAnswer = function () {
        var _this = this;
        this.server.getAnswer()
            .subscribe(function (answer) { return _this.txtField += answer; }, function (error) { return _this.errorMessage = error; });
        this.txtField += '\n';
    };
    BiBiGridGui.prototype.sendFlags = function () {
        var _this = this;
        if (!this.readFields()) {
            return;
        }
        this.server.sendFlags(this.readFields())
            .subscribe(function (flags) { return console.log(flags); }, function (error) { return _this.errorMessage = error; });
        this.getAnswer();
    };
    BiBiGridGui.prototype.scrollDown = function () {
        var textArea = document.getElementById("output");
        textArea.scrollTop = textArea.scrollHeight;
    };
    BiBiGridGui.prototype.createInputField = function (name, type) {
        var field = "";
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
                "";
        }
        return field;
    };
    return BiBiGridGui;
}());
BiBiGridGui = __decorate([
    core_1.Component({
        selector: 'bibigui',
        template: "\n<welcome></welcome>\n<!--<mainpage></mainpage>-->\n<!--<navbar ></navbar>-->\n<!--<div class=\"container-fluid text-center\" *ngIf='userSet'>\n\n<h1>BiBiGrid</h1> \n<h3><small>BiBiGrid is a tool for an easy cluster setup inside a cloud environment. It is written in Java and run on any OS a Java runtime is provided - any Java 8 is supported. \nBiBiGrid and its Cmdline UI based on a general cloud provider api. Currently there exists implementations for Amazon (AWS EC2 using the official AWS SDK) and OpenStack (Nova using OpenStack4J). \nBiBiGrid offers an easy configuration and maintenance of a started cluster via command-line.</small></h3>\n</div>\n<br>\n<br>\n<div class=\"container-fluid\"> \n<table class=\"table table-inverse table-striped table-bordered table-hover\">\n<thead>\n      <tr>\n        <th>Short flag</th>\n        <th>Long flag</th>\n        <th>Description</th>\n        <th>Input</th>\n      </tr>\n    </thead>\n<tbody>\n<tr *ngFor=\"let flag of flags\"><th>{{flag.sFlag}}</th><th>{{flag.lFlag}}</th><th>{{flag.sDescription}}</th><th [ngSwitch]=\"flag.type\"><div class=\"input-group\">\n                                                                                                                                        <input *ngSwitchCase=\"'string'\" class=\"form-control\" type=\"text\" id=\"{{flag.sFlag}}\">\n                                                                                                                                        <input *ngSwitchCase=\"'int'\" class=\"form-control\" type=\"number\" id=\"{{flag.sFlag}}\">\n                                                                                                                                        <input *ngSwitchCase=\"'float'\" class=\"form-control\" type=\"number\" step=\"any\" id=\"{{flag.sFlag}}\">\n                                                                                                                                        <input *ngSwitchDefault class=\"form-control\" type=\"text\" id=\"{{flag.sFlag}}\">\n                                                                                                                                        <span class=\"input-group-addon\" id=\"addon-type\">{{flag.type}}</span></div></th></tr>\n\n</tbody>\n</table>\n\n<button class=\"btn btn-primary btn-lg btn-block\" type=\"submit\" (click)=\"sendFlags()\" id=\"submit\">\n    <span class=\"glyphicon glyphicon-send\" aria-hidden=\"true\"></span> Submit\n</button>\n<br>\n<br>\n <div class=\"form-group\">\n  <label for=\"output\"><h3>Output:</h3></label>\n  <textarea class=\"form-control\" rows=\"10\" id=\"output\" disabled=\"true\">{{txtField}}</textarea>\n</div>\n</div>-->\n",
        providers: [app_server_communication_1.ServerCommunication, app_bibigrid_gui_navbar_1.BiBiGridGuiNavbar]
    }),
    __metadata("design:paramtypes", [app_server_communication_1.ServerCommunication])
], BiBiGridGui);
exports.BiBiGridGui = BiBiGridGui;
//# sourceMappingURL=app.bibigrid-gui.js.map