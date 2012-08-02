/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/*
 * Dependencies
 *
 * /dev/lib/jquery/plugins/jqmodal.sakai-edited.js
 */

/*global, fluid, window, $ */

require(["jquery", "sakai/sakai.api.core"], function($, sakai) {

    /**
     * @name sakai_global.emailverify
     *
     * @class emailverify
     *
     * @description
     * emailverify widget
     *
     * @version 0.0.1
     * @param {String} tuid Unique id of the widget
     * @param {Boolean} showSettings Show the settings of the widget or not
     */
    sakai_global.emailverify = function(tuid, showSettings){


        /////////////////////////////
        // Configuration variables //
        /////////////////////////////


        // classes
        var buttonDisabled = "s3d-disabled";

        ///////////////////
        // CSS Selectors //
        ///////////////////

        var $rootel = $("#" + tuid);

        var errorMode;
        var emailChangeForm;

        var $emailverify_action_resend = $("#emailverify_resend_email_button", $rootel);
        var $emailverify_action_continue = $("#emailverify_continue_button", $rootel);
        var $emailverify_dialog = $("#emailverify_dialog", $rootel);

        var $accountpreferences_email_tab = $("#accountpreferences_email_tab", $rootel);

        var emailTxt = "#accountpreferences_email";

        // Message titles
        var emailverify_email_resent_title = $("#emailverify_email_resent_title", $rootel);
        var emailverify_email_resend_failed_title = $("#emailverify_email_resend_failed_title", $rootel);
        var emailverify_email_verified_title = $("#emailverify_email_verified_title", $rootel);
        var emailverify_email_change_failed_title = $("#emailverify_email_change_failed_title", $rootel);
        var emailverify_email_cancel_change_success_title = $("#emailverify_email_cancel_change_success_title", $rootel);
        var emailverify_email_cancel_change_failed_title = $("#emailverify_email_cancel_change_failed_title", $rootel);

        var emailverify_email_taken = $("#emailverify_email_taken", $rootel);

        var emailverify_email_title = $("#emailverify_email_title", $rootel);

        var emailverify_email_invalid_url_title = $("#emailverify_email_invalid_url_title", $rootel);
        var emailverify_email_invalid_url = $("#emailverify_email_invalid_url", $rootel);

        var emailverify_change_email_template = $("#emailverify_change_email_template", $rootel);
        var emailverify_dialog_template = $("#emailverify_dialog_template", $rootel);

        /////////////
        // Binding //
        /////////////

        /**
         * Disable or enable elements
         * can take a single or multivalue jQuery obj
         */
        var enableElements = function (jQueryObj) {
            jQueryObj.removeAttr("disabled");
            jQueryObj.removeClass(buttonDisabled);
        };

        var disableElements = function (jQueryObj) {
            jQueryObj.attr("disabled", "disabled");
            jQueryObj.addClass(buttonDisabled);
        };

        /**
         * Add binding to the various element in the accept terms widget
         */
        var addBinding = function(){

            if (errorMode) {
                $("#emailverify_email_title", $rootel).html(emailverify_email_invalid_url_title.html());
                $("#email_not_verified_msg", $rootel).html(emailverify_email_invalid_url.html());
            }

            // bind to changed events for the user data flag to record if the use changed the data.
            changedUserData = "unchanged";

            // Reinitialise the jQuery selector
            $emailverify_action_resend = $($emailverify_action_resend.selector);

            // Add binding to the resend button
            $emailverify_action_resend.unbind("click").bind("click", function () {
                $.ajax({
                     url: "/~" + sakai.data.me.user.userid + "/emailVerify.resend.json",
                     type: "GET",
                     dataTypeString: "json",
                     success : function(data) {
                        sakai.api.Util.notification.show(emailverify_email_resent_title.html(),
                            sakai.api.i18n.getValueForKey("EMAIL_RESENT_NOTIFICATION", "emailverify")
                                .replace("${email}", sakai.data.me.user.properties.newemail?sakai.data.me.user.properties.newemail:sakai.data.me.user.properties.email));
                     },
                     error : function(status) {
                         sakai.api.Util.notification.show(emailverify_email_resend_failed_title.html(),
                                 sakai.api.i18n.getValueForKey("EMAIL_RESEND_FAILED", "emailverify"));
                     }
                });

                $emailverify_dialog.jqmHide();
                return false;
            });

            // Reinitialise the jQuery selector
            $emailverify_action_continue = $($emailverify_action_continue.selector);

            // Add binding to the don't accept button
            $emailverify_action_continue.unbind("click").bind("click", function () {
                $emailverify_dialog.jqmHide();
                return false;
            });
        };

        var showVerifyWarning  = function () {

            if (sakai.data.me.user.anon) {
              return false;
            }

            if ( sakai.data.me.user.properties.emailverified ) {
               return false;
            }

            if (sakai.data.me.user.properties.emailverifyerror) {
                errorMode = true;
            }
            else if (sakai.data.me.user.properties.emailverifyseewarning > new Date().getTime()) {
                return false;
            }

            return true;
         };

         var justVerified  = function () {
             if (sakai.data.me.user.anon) {
               return false;
             }
             if ( sakai.data.me.user.properties.emailverified
                     && window.location.hash == '#!emailVerified') {
                return true;
             }
             return false;
          };

        var myClose=function(hash) {
           hash.w.fadeOut('2000',function(){ hash.o.remove(); });
        };


        var popUpVerify = function() {
            verifyDate = new Date(new Number(sakai.data.me.user.properties.emailverifyby));
            var data = $.extend({}, sakai.data.me.user.properties,
                    {
                        "date": verifyDate.toLocaleDateString(),
                        "time": verifyDate.toLocaleTimeString()
                    });

            $emailverify_dialog.html(sakai.api.Util.TemplateRenderer(emailverify_dialog_template, {
                "config": null,
                "data": data,
                "parentid": "0",
                sakai: sakai
            }));

            var useEmail = sakai.data.me.user.properties.email;

            if (sakai.data.me.user.properties.newemail) {
                useEmail = sakai.data.me.user.properties.newemail;
            }

            $emailverify_dialog.jqm({
                modal: true,
                toTop: true,
                onHide : myClose
             });
             addBinding();
             $emailverify_dialog.jqmShow();

             $.ajax({
                 url: "/~" + sakai.data.me.user.userid + "/emailVerify.seenWarning.json",
                 type: "GET",
                 dataType: "json",
                 success : function(data) {
                 },
                 error : function(status) {
                 }
             });
        };

        var checkEmailAddress = function(email) {
            var url = sakai.config.URL.USER_EMAIL_EXISTENCE_SERVICE.replace(/__EMAIL__/g, email);
            var errObj = new Array();
            return sakai.api.User.checkExistence(url, false, null, errObj);
        };

        var doUpdateEmail = function() {
            var email = $(emailTxt).val();

            if (!checkEmailAddress(email)) {
                $(emailTxt).next().text(emailverify_email_taken.text());
                $(emailTxt).next().show();
                return;
            }

            var object = {
                    "email": email
            };

            // emailChange
            $.ajax({
                url: "/~" + sakai.data.me.user.userid + "/emailVerify.emailChange.json",
                type: "POST",
                data: object,
                dataType: "json",
                success : function(data) {
                    sakai.data.me.user.properties.emailverified = false;
                    sakai.data.me.user.properties.newemail = email;
                    if ($("#accountpreferences_container").length) {
                        $("#accountpreferences_container").jqmHide();
                    }
                    popUpVerify();
                    decorateEmailForm(emailChangeForm);
                },
                error : function(status) {
                    sakai.api.Util.notification.show(emailverify_email_change_failed_title.html(),
                            sakai.api.i18n.getValueForKey("EMAIL_CHANGE_FAILED", "emailverify"));
                    $(emailTxt).val(sakai.data.me.user.properties.email);
                }
            });
        };

        var decorateEmailForm = function(emailForm) {
            emailChangeForm = emailForm;
            emailForm.addClass("emailverify_processed");
            emailForm.html(sakai.api.Util.TemplateRenderer(emailverify_change_email_template, {
                "config": null,
                "data": sakai.data.me.user.properties,
                "parentid": "0",
                sakai: sakai
            }));

            $.validator.addMethod("newemail", function(value, element){
                return value != sakai.data.me.user.properties.email;
            }, "type a new email");

            $("#accountpreferences_email_change").validate({
                onclick: false,
                onkeyup: false,
                onfocusout: false,
                errorClass: "signup_form_error",
                rules: {
                    email: {
                        email: true,
                        required: true,
                        newemail: true
                    }
                },
                submitHandler: function(form, validator){
                    doUpdateEmail();
                    return false;
                },
                errorPlacement: function(error, element){
                    // don't show an error...
                }
            });

            $('#accountpreferences_email').keyup(function() {
                $(".accountpreferences_error").hide("");
                if ($("#accountpreferences_email_change").valid()) {
                    enableElements($("#accountpreferences_saveEmail"));
                }
                else {
                    disableElements($("#accountpreferences_saveEmail"));
                }
            });

            disableElements($("#accountpreferences_saveEmail"));

            if (emailForm.find(".emailverify_cancel_change_request").length == 1) {
                emailForm.find(".emailverify_cancel_change_request").click(
                    function(){
                        // cancelEmailChange
                        var object = {};
                        $.ajax({
                            url: "/~" + sakai.data.me.user.userid + "/emailVerify.cancelEmailChange.json",
                            type: "POST",
                            data: object,
                            dataType: "json",
                            success : function(data) {
                                sakai.data.me.user.properties.emailverified = true;
                                sakai.api.Util.notification.show(emailverify_email_cancel_change_success_title.html(),
                                        sakai.api.i18n.getValueForKey("EMAIL_CHANGE_CANCEL_SUCCESS", "emailverify")
                                        .replace("${email}", sakai.data.me.user.properties.email));
                                decorateEmailForm(emailChangeForm);
                            },
                            error : function(status) {
                                sakai.api.Util.notification.show(emailverify_email_cancel_change_failed_title.html(),
                                        sakai.api.i18n.getValueForKey("EMAIL_CHANGE_CANCEL_FAILED", "emailverify"));
                            }
                        });
                    });
            }

            if (emailForm.find(".emailverify_pop_verify_warning").length == 1) {
                $(".emailverify_pop_verify_warning").click(function(){
                    if ($("#accountpreferences_container").length) {
                        $("#accountpreferences_container").jqmHide();
                    }
                    popUpVerify();
                });
            }
        };

        ////////////////////
        // Initialisation //
        ////////////////////

        /**
         * Initialize the email verify widget
         * All the functionality in here is loaded before the widget is actually rendered
         */
        var init = function(){
            if ( showVerifyWarning() ) {
               // This will make the widget popup as a layover.
               popUpVerify();
            }
            else if (justVerified()) {
                sakai.api.Util.notification.show(emailverify_email_verified_title.html(),
                        sakai.api.i18n.getValueForKey("EMAIL_VERIFIED_MESSAGE", "emailverify"));
            }
        };

        if (sakai.config.emailVerifyEnabled && !sakai.data.me.user.anon) {
            if ($("div#accountpreferences_changeEmailContainer, emailverify_processed").length > 0) {
                decorateEmailForm($("div#accountpreferences_changeEmailContainer"));
            }
            else {
                $("*").delegate("div#accountpreferences_changeEmailContainer, emailverify_processed", "load", function() {
                    decorateEmailForm($(this));
                });
            }

            init();
        }
    };

    sakai.api.Widgets.widgetLoader.informOnLoad("emailverify");
});
