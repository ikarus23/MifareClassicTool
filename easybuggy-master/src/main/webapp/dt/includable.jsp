<%@ page pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="${language}" />
<fmt:setBundle basename="messages" />
<!DOCTYPE html>
<html>
<head>
<title><fmt:message key="title.design.test.page" /></title>
<link rel="icon" type="image/vnd.microsoft.icon" href="${pageContext.request.contextPath}/images/favicon.ico">
<c:import url="/dfi/style_bootstrap.html" />
</head>
<body style="margin-left: 20px; margin-right: 20px;">
    <table style="width: 100%;">
        <tr>
            <td>
                <h2>
                    <span class="glyphicon glyphicon-globe"></span>&nbsp;
                    <fmt:message key="title.design.test.page" />
                </h2>
            </td>
            <td align="right"><a href="${pageContext.request.contextPath}/"><fmt:message key="label.go.to.main" /></a></td>
        </tr>
    </table>
    <hr style="margin-top: 0" />
    <!-- header section start -->
    <c:catch var="ex">
        <c:if test="${param.template != null && !fn:startsWith(param.template,'http')}">
            <c:import url="<%=request.getParameter(\"template\") +\"_header.html\"%>" />
        </c:if>
    </c:catch>
    <!-- header section end -->
    <p>
        <fmt:message key="description.design.test" />
    </p>
    <ul>
        <li><p>
                <a href="includable.jsp"><fmt:message key="style.name.noframe" /></a>:
                <fmt:message key="style.description.noframe" />
            </p></li>
        <li><p>
                <a href="includable.jsp?template=basic"><fmt:message key="style.name.basic" /></a>:
                <fmt:message key="style.description.basic" />
            </p></li>
        <li><p>
                <a href="includable.jsp?template=monochro"><fmt:message key="style.name.monochro" /></a>:
                <fmt:message key="style.description.monochro" />
            </p></li>
    </ul>
    <br>
    <div class="alert alert-info" role="alert">
        <span class="glyphicon glyphicon-info-sign"></span>&nbsp;
        <fmt:message key="msg.note.path.traversal" />
    </div>
    <!-- footer section start -->
    <c:catch var="ex">
        <c:if test="${param.template != null && !fn:startsWith(param.template,'http')}">
            <c:import url="<%=request.getParameter(\"template\") +\"_footer.html\"%>" />
        </c:if>
    </c:catch>
    <!-- footer section end -->
</body>
</html>
