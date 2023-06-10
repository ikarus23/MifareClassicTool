<%@ page pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="${language}" />
<fmt:setBundle basename="messages" />
<!DOCTYPE HTML>
<html>
<head>
<title><fmt:message key="title.clientinfo.page" /></title>
<link rel="icon" type="image/vnd.microsoft.icon" href="${pageContext.request.contextPath}/images/favicon.ico">
<c:import url="/dfi/style_bootstrap.html" />
</head>
<body style="margin-left: 20px; margin-right: 20px;">
    <table style="width: 100%;">
        <tr>
            <td>
                <h2>
                    <span class="glyphicon glyphicon-globe"></span>&nbsp;
                    <fmt:message key="title.clientinfo.page" />
                </h2>
            </td>
            <td align="right"><a href="${pageContext.request.contextPath}/"><fmt:message key="label.go.to.main" /></a></td>
        </tr>
    </table>
    <hr style="margin-top: 0" />
    <table class="table table-striped table-bordered table-hover" style="font-size: small;">
        <tr>
            <th><fmt:message key="label.key" /></th>
            <th><fmt:message key="label.value" /></th>
        </tr>
        <tr>
            <td><fmt:message key="label.code" /></td>
            <td id="appCodeName"></td>
        </tr>
        <tr>
            <td><fmt:message key="label.browser" /></td>
            <td id="appName"></td>
        </tr>
        <tr>
            <td><fmt:message key="label.version" /></td>
            <td id="appVersion"></td>
        </tr>
        <tr>
            <td><fmt:message key="label.platform" /></td>
            <td id="platform"></td>
        </tr>
        <tr>
            <td><fmt:message key="label.user.agent" /></td>
            <td id="userAgent"></td>
        </tr>
        <tr>
            <td><fmt:message key="label.language" /></td>
            <td id="browserLanguage"></td>
        </tr>
    </table>
    <hr />
    <div class="alert alert-info" role="alert">
        <span class="glyphicon glyphicon-info-sign"></span>&nbsp;
        <fmt:message key="msg.note.clientinfo" />
    </div>
    <script type="text/javascript">
                <!--
                    document.getElementById("appCodeName").textContent = navigator.appCodeName;
                    document.getElementById("appName").textContent = navigator.appName;
                    document.getElementById("appVersion").textContent = navigator.appVersion;
                    document.getElementById("platform").textContent = navigator.platform;
                    document.getElementById("userAgent").textContent = navigator.userAgent;
                    document.getElementById("browserLanguage").textContent = navigator.language;
                // -->
                </script>
</body>
</html>