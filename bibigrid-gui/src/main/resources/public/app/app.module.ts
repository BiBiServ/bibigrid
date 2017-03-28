

import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {HttpModule,JsonpModule} from '@angular/http';
import {FormsModule} from '@angular/forms';

import { ModalModule } from 'angular2-modal';
import { BootstrapModalModule } from 'angular2-modal/plugins/bootstrap';

import {BiBiGridGui} from './app.bibigrid-gui.component';
import {ServerCommunication} from './server/app.server-communication';
import {BiBiGridGuiNavbar} from './core/app.bibigrid-gui.navbar';
import {welcomePage} from './welcome_page/app.welcome-page.component';
import {SafeHtml} from "./shared/app.insert-html.pipe";
import {mainPage} from "./core/app.main.component";
import {configLoader} from './core/app.config-loader.component';

@NgModule ( {
    imports: [BrowserModule,HttpModule,JsonpModule,FormsModule,BootstrapModalModule,ModalModule.forRoot()],
    declarations: [BiBiGridGui,BiBiGridGuiNavbar,SafeHtml,welcomePage, mainPage,configLoader],
    bootstrap: [BiBiGridGui],
    providers: [ServerCommunication]
})
export class AppModule {}