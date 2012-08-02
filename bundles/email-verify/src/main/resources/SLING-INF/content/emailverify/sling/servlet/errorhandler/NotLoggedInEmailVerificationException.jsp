<%@ page session="false" %><!DOCTYPE HTML>
<html xmlns="http://www.w3.org/1999/xhtml">

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title></title>

        <!-- Sakai Core CSS -->
        <link rel="stylesheet" type="text/css" href="/dev/css/sakai/main.css" />
        <link rel="stylesheet" type="text/css" href="/dev/css/sakai/sakai.corev1.css" />

        <!-- Sakai Error CSS -->
        <link rel="stylesheet" type="text/css" href="/dev/css/sakai/sakai.error.css" />
        <link rel="stylesheet" type="text/css" href="/devwidgets/emailverify/css/emailverify_error.css" />

    </head>

    <body class="fl-centered i18nable">
    
        <div class="fl-container-flex header s3d-header">
            <div class="s3d-navigation-container">
                <div id="widget_topnavigation" class="widget_inline"></div>
            </div>
            <div class="fl-fix fl-centered fixed-container s3d-fixed-container">
                <div class="s3d-container-shadow-left"><!----></div>
                <div class="s3d-container-shadow-right"><!----></div>
                <div class="s3d-page-header">
                    <div id="widget_institutionalskinning" class="widget_inline"></div>
                </div>
            </div>
        </div>
        <div class="fl-fix fl-centered fixed-container s3d-main-container">
            <div id="error_content">
                <div id="error_content_second_column">
                    <div id="error_sign_in_button">
                        <p class="error_signin_button"><button>__MSG__SIGN_IN__</button></p>
                        <p>__MSG__NO_ACCOUNT__ <a class="s3d-regular-links s3d-bold" href="/register">__MSG__SIGN_UP__</a></p>
                    </div>
                </div>
                <div id="error_content_first_column">
                    <div id="error_content_first_column_content">
                        <h1>You are not logged in</h1>
                        <p>Please sign in first, then click the verification link in your email again.</p>
                    </div>
                </div>
            </div>
        </div>
        <!-- FOOTER WIDGET -->
        <div id="widget_footer" class="widget_inline footercontainer"></div>

        <!-- Dependency JS -->
        <script data-main="/dev/lib/sakai/sakai.dependencies.js" src="/dev/lib/jquery/require.js"></script>

        <script>require(["/devwidgets/emailverify/javascript/emailverify-error.js"]);</script>

    </body>
</html>
