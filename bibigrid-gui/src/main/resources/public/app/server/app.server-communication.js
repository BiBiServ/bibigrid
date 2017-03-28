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
var core_1 = require('@angular/core');
var http_1 = require('@angular/http');
var http_2 = require('@angular/http');
var Observable_1 = require('rxjs/Observable');
require('rxjs/add/operator/map');
require('rxjs/add/operator/catch');
require('rxjs/add/observable/throw');
var json_wrapper_1 = require('./json.wrapper');
var ServerCommunication = (function () {
    function ServerCommunication(http) {
        this.http = http;
        this.getUrl = "http://localhost:8080/resource";
        this.sendUrl = "http://localhost:8080//process";
        this.ansUrl = "http://localhost:8080/answer";
    }
    ServerCommunication.prototype.getConfig = function (confPath) {
        return this.http.get(confPath)
            .map(this.extractData)
            .catch(this.handleError);
    };
    ServerCommunication.prototype.getConfLinks = function (confPath) {
        return this.http.get(confPath)
            .map(this.extractData)
            .catch(this.handleError);
    };
    ServerCommunication.prototype.getFlags = function () {
        return this.http.get(this.getUrl)
            .map(this.extractData)
            .catch(this.handleError);
    };
    ServerCommunication.prototype.getAnswer = function () {
        return this.http.get(this.ansUrl)
            .map(this.extractData)
            .catch(this.handleError);
    };
    ServerCommunication.prototype.sendFlags = function (flags) {
        var headers = new http_2.Headers({ 'Content-Type': 'application/json' });
        var options = new http_2.RequestOptions({ headers: headers });
        return this.http.post(this.sendUrl, JSON.stringify(new json_wrapper_1.JsonWrapper(flags)), options)
            .map(this.extractData)
            .catch(this.handleError);
    };
    ServerCommunication.prototype.extractData = function (res) {
        var body = res.json();
        console.log(body);
        return body.data || {};
    };
    ServerCommunication.prototype.handleError = function (error) {
        var errMsg;
        if (error instanceof http_1.Response) {
            var body = error.json() || '';
            var err = body.error || JSON.stringify(body);
            errMsg = error.status + " - " + (error.statusText || '') + " " + err;
        }
        else {
            errMsg = error.message ? error.message : error.toString();
        }
        console.error(errMsg);
        return Observable_1.Observable.throw(errMsg);
    };
    ServerCommunication = __decorate([
        core_1.Injectable(), 
        __metadata('design:paramtypes', [http_1.Http])
    ], ServerCommunication);
    return ServerCommunication;
}());
exports.ServerCommunication = ServerCommunication;
//# sourceMappingURL=app.server-communication.js.map