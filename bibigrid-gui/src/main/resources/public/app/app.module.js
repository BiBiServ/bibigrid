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
var platform_browser_1 = require('@angular/platform-browser');
var http_1 = require('@angular/http');
var forms_1 = require('@angular/forms');
var angular2_modal_1 = require('angular2-modal');
var bootstrap_1 = require('angular2-modal/plugins/bootstrap');
var app_bibigrid_gui_component_1 = require('./app.bibigrid-gui.component');
var app_server_communication_1 = require('./server/app.server-communication');
var app_bibigrid_gui_navbar_1 = require('./core/app.bibigrid-gui.navbar');
var app_welcome_page_component_1 = require('./welcome_page/app.welcome-page.component');
var app_insert_html_pipe_1 = require("./shared/app.insert-html.pipe");
var app_main_component_1 = require("./core/app.main.component");
var app_config_loader_component_1 = require('./core/app.config-loader.component');
var AppModule = (function () {
    function AppModule() {
    }
    AppModule = __decorate([
        core_1.NgModule({
            imports: [platform_browser_1.BrowserModule, http_1.HttpModule, http_1.JsonpModule, forms_1.FormsModule, bootstrap_1.BootstrapModalModule, angular2_modal_1.ModalModule.forRoot()],
            declarations: [app_bibigrid_gui_component_1.BiBiGridGui, app_bibigrid_gui_navbar_1.BiBiGridGuiNavbar, app_insert_html_pipe_1.SafeHtml, app_welcome_page_component_1.welcomePage, app_main_component_1.mainPage, app_config_loader_component_1.configLoader],
            bootstrap: [app_bibigrid_gui_component_1.BiBiGridGui],
            providers: [app_server_communication_1.ServerCommunication]
        }), 
        __metadata('design:paramtypes', [])
    ], AppModule);
    return AppModule;
}());
exports.AppModule = AppModule;
//# sourceMappingURL=app.module.js.map