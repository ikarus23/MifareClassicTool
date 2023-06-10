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
<title><fmt:message key="title.serverinfo.page" /></title>
<link rel="icon" type="image/vnd.microsoft.icon" href="${pageContext.request.contextPath}/images/favicon.ico">
<c:import url="/dfi/style_bootstrap.html" />
</head>
<body style="margin-left: 20px; margin-right: 20px;">
    <table style="width: 100%;">
        <tr>
            <td>
                <h2>
                    <span class="glyphicon glyphicon-globe"></span>&nbsp;
                    <fmt:message key="title.serverinfo.page" />
                </h2>
            </td>
            <td align="right"><fmt:message key="label.login.user.id" />: <%=session.getAttribute("userid")%> <br> <a
                href="${pageContext.request.contextPath}/logout"><fmt:message key="label.logout" /></a></td>
        </tr>
    </table>
    <hr style="margin-top: 0" />
    <%
        request.setAttribute("systemProperties", java.lang.System.getProperties());
    %>
    <table style="width: 720px;" class="table table-striped table-bordered table-hover" style="font-size:small;">
        <tr>
            <th><fmt:message key="label.key" /></th>
            <th><fmt:message key="label.value" /></th>
        </tr>
        <c:forEach var="entry" items="${systemProperties}">
            <tr>
                <td><c:out value="${entry.key}" /></td>
                <td><c:out value="${entry.value}" /></td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>