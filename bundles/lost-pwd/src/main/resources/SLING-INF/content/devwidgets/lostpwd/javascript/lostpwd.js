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
     * @name sakai_global.lostpwd
     *
     * @class lostpwd
     *
     * @description
     * lostpwd widget
     *
     * @version 0.0.1
     * @param {String} tuid Unique id of the widget
     * @param {Boolean} showSettings Show the settings of the widget or not
     */
    sakai_global.lostpwd = function(tuid, showSettings){
    	
        /////////////////////////////
        // Configuration variables //
        /////////////////////////////

    	var changePwdPrefix = "#!lostpwdurl:";
    	var lostPwdFailPrefix = "#!lostpwdfail:"
    	

        // classes
        var buttonDisabled = "s3d-disabled";

        ///////////////////
        // CSS Selectors //
        ///////////////////

        var $rootel = $("#" + tuid);
        
        /*
        var $emailverify_action_resend = $("#emailverify_resend_email_button", $rootel);
        var $emailverify_action_continue = $("#emailverify_continue_button", $rootel);
        var $emailverify_dialog = $("#emailverify_dialog", $rootel);
       */
        
        var $lostpwd_change_dialog = $("#lostpwd_change_dialog", $rootel);
        
        var $lostpwd_recover_pwd = $("#lostpwd_recover_pwd", $rootel);
        
        var $lostpassword_change = $("#lostpassword_change", $rootel);
        var $passChangeContainer =  $("#lostpassword_changePassContainer", $rootel);
        
        var lostPasswordID = "#lostpassword"
        
        var errorFailChangePass = lostPasswordID + "_error_failChangePass";
        var errorFailChangePassBody = lostPasswordID + "_error_failChangePassBody";
        var messagePassChanged = lostPasswordID + "_message_passChanged";
        var messagePassChangedBody = lostPasswordID + "_message_passChangedBody";
        
        var lostpassword_reset_success = "#lostpassword_reset_success";
        var lostpassword_reset_success_body = "#lostpassword_reset_success_body";
        
        var errorFailRecover = lostPasswordID + "_error_failRecover";
        var errorFailRecoverBody = lostPasswordID + "_error_failRecoverBody";
        
        var errorFailReset = lostPasswordID + "_error_fail_Reset";
        
        var lostpwd_recover_pwd_username_email = "#lostpwd_recover_pwd_username_email";
        
        // Textboxes
        var newPassTxt = "#new_pass";
        var newRetypePassTxt = "#retype_pass";

        var saveNewPass = "#lostpassword_saveNewPass";
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
         * Check if the input given by the user is valid
         * @return {Boolean} true if input is valid
         */
        var checkIfInputValid =function(){
            var newPass1 = $(newPassTxt).val();
            var newPass2 = $(newRetypePassTxt).val();

            // check if the user didn't just fill in some spaces
            return (newPass1.replace(/ /g, "") !== "" && newPass2.replace(/ /g, "") !== "");
        };

        /**
         * Clears all the password fields
         */
        var clearPassFields = function(){
            $(newPassTxt).val("");
            $(newRetypePassTxt).val("");
        };

        /**
         * Makes all the checks
         * are the new passwords equal
         *
         */
        var changePass = function(){
            var newPass1 = $(newPassTxt).val();
            var newPass2 = $(newRetypePassTxt).val();

                /*
                 * password : the new password
                 */

            var lostPwdUrl = window.location.hash.substr(changePwdPrefix.length);
            
                $.ajax({
                    url :lostPwdUrl + "&password=" + encodeURIComponent(newPass1),
                    type : "GET",
                    success : function(data) {
                        // show successful password change message through gritter
                        sakai.api.Util.notification.show($(messagePassChanged).html(), $(messagePassChangedBody).html());
                        // clear all the fields
                        clearPassFields();
                        
                        setTimeout( function() {
                            window.location.href = "/me";
                        }, 2000);
                    },
                    error: function(xhr, textStatus, thrownError) {
                        // show error message through gritter
                        sakai.api.Util.notification.show($(errorFailChangePass).html(), $(errorFailChangePassBody).html());
                        // clear all the fields
                        clearPassFields();
                    }
                });
        };


        /**
         * Initialise form validation
         */
        var initValidation = function(){
            $lostpassword_change.validate({
                errorClass: "lostpassword_error",
                errorElement:"div",
                rules:{
                    new_pass:{
                        required: true,
                        minlength: 4
                    },
                    retype_pass:{
                        required: true,
                        minlength: 4,
                        equalTo: "#new_pass"
                    }
                },
                messages: {
                    retype_pass:{
                        "equalTo": "Please enter the same password twice."
                    }
                },
                debug:true

            });
        };

        /** Binds the submit function on the password change form **/
        $lostpassword_change.submit(function(){

            var newPass1 = $(newPassTxt).val();
            var newPass2 = $(newRetypePassTxt).val();

            // check if the user enter valid data for old and new passwords
            if ($lostpassword_change.valid()) {

                // change the password
                changePass();
            }
            return false;
        });

        /** Binds all the password boxes (keyup) **/
        $("input[type=password]", $passChangeContainer).keyup(function(e){

            // If we'd use keypress for this then the input fields wouldn't be updated yet
            // check if the user didn't just fill in some spaces
            if(checkIfInputValid()){
                // enable the change pass button
                enableElements($(saveNewPass));
            }
            else{
                // disable the change pass button
                disableElements($(saveNewPass));
            }
        });

        var showChangePwd  = function () {
        	var hash = window.location.hash;
        	
            if (sakai.data.me.user.anon && hash.match("^"+changePwdPrefix)==changePwdPrefix) {
              return true;
            }
            return false;
         };
        
         var popUpPasswordChange = function() {
        	 initValidation();
             
        	 // disable the change pass button
             disableElements($(saveNewPass));

             $lostpwd_change_dialog.jqm({
                 modal: true,
                 toTop: true,
                 onHide : myClose
              });
        	 
        	 $lostpwd_change_dialog.jqmShow();
         }
         
        var myClose=function(hash) { 
           hash.w.fadeOut('2000',function(){ hash.o.remove(); }); 
        }; 

        var doRecoverPassword = function() {
            var username = $("#lostpwd_recover_pwd_username_email").val();
                $.ajax({
                    url :"/system/lostpasswordfind?username=" + encodeURIComponent(username),
                    type : "GET",
                    success : function(data) {
                        // show successful password change message through gritter
                        sakai.api.Util.notification.show($(lostpassword_reset_success).html(), $(lostpassword_reset_success_body).html());
                    },
                    error: function(xhr, textStatus, thrownError) {
                    	$errorBody = $("#lostpwd_reset_error_" + xhr.status);
                    	
                        // show error message through gritter
                        sakai.api.Util.notification.show($(errorFailReset).html(), $errorBody.html());
                    }
                });
        }
        
        var decorateSigninRecoverPwd = function(resetButton) {
        	resetButton.addClass("lostpwd_processed");

        	resetButton.parent().append($lostpwd_recover_pwd);
        	
        	$("#lostpwd_recover_pwd_form").validate({
                errorClass: "lostpassword_reset_error",
                errorElement:"div",
                rules: {
                	lostpwd_recover_pwd_username_email: {
                		required: true
                	}
                },
                submitHandler: function(form, validator){
                    doRecoverPassword();
                    return false;
                }
            });
        	
        	resetButton.click(function () {
        		$lostpwd_recover_pwd.slideToggle('slow');
        		$(lostpwd_recover_pwd_username_email).focus();
        		
        		return false;
        	});
        }
        
        if ($("#topnavigation_reset_pwd_link, lostpwd_processed").length > 0) {
        	decorateSigninRecoverPwd($("#topnavigation_reset_pwd_link, lostpwd_processed"));
        }
        else {
            $("*").delegate("#topnavigation_reset_pwd_link, lostpwd_processed", "load", function() {
            	decorateSigninRecoverPwd($(this));
            });       	
        }

        ////////////////////
        // Initialisation //
        ////////////////////

        /**
         * Initialize the email verify widget
         * All the functionality in here is loaded before the widget is actually rendered
         */
        var init = function(){
        	
        	
        	if (showChangePwd()) {
        		popUpPasswordChange();
        	}
        	else if (window.location.hash.match("^" + lostPwdFailPrefix) == lostPwdFailPrefix) {
        		var failType = 
                sakai.api.Util.notification.show($(errorFailRecover).html(), 
                		$(errorFailRecoverBody + "_" + window.location.hash.substr(lostPwdFailPrefix.length)).html());
        	}

        };

        init();
    };

    sakai.api.Widgets.widgetLoader.informOnLoad("lostpwd");
});
