<%@ page pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page import="java.util.ResourceBundle"%>
<c:set var="language" value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" scope="session" />
<fmt:setLocale value="${language}" />
<fmt:setBundle basename="indexpage" />
<%!boolean isFirstLoad = true;%>
<%
    session.removeAttribute("dlpinit");
    ResourceBundle rb = ResourceBundle.getBundle("messages", request.getLocale());
    String permName = rb.getString("label.metaspace");
    String permNameInErrorMsg = permName;
    String javaVersion = System.getProperty("java.version");
    if (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7")) {
        permName = rb.getString("label.permgen.space");
        permNameInErrorMsg = "PermGen space";
    }
    String mode = System.getProperty("easybuggy.mode");
    boolean isOnlyVulnerabilities = mode != null && mode.equalsIgnoreCase("only-vulnerabilities");
%>
<!DOCTYPE html>
<html>
<head>
<title>EasyBuggy</title>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/3.5.2/animate.min.css"
    integrity="sha384-OHBBOqpYHNsIqQy8hL1U+8OXf9hH6QRxi0+EODezv82DfnZoV7qoHAZDwMwEJvSw" crossorigin="anonymous">
<link rel="icon" type="image/vnd.microsoft.icon" href="${pageContext.request.contextPath}/images/favicon.ico">
<c:import url="/dfi/style_bootstrap.html" />
</head>
<body style="margin-top: 20px; margin-left: 20px; margin-right: 20px;">
    <header>
        <table style="width: 720px;">
            <tr>
                <td><img src="images/easybuggy.png" class="bounceInRight animated" width="324px" height="116px" /></td>
                <td><fmt:message key="description.all" /></td>
            </tr>
        </table>
    </header>
    <hr>
    <%
        if (!isOnlyVulnerabilities) {
    %>
    <h2>
        <span class="glyphicon glyphicon-knight"></span>&nbsp;
        <fmt:message key="section.troubles" />
    </h2>
    <p>
        <fmt:message key="description.troubles" />
    </p>
    <ul>
        <li><p>
                <a href="deadlock"><fmt:message key="function.name.dead.lock" /></a>:
                <fmt:message key="function.description.dead.lock" />
            </p></li>
        <li><p>
                <a href="deadlock2"><fmt:message key="function.name.dead.lock2" /></a>:
                <fmt:message key="function.description.dead.lock2" />
            </p></li>
        <li><p>
                <a href="endlesswaiting"><fmt:message key="function.name.endless.waiting.process" /></a>:
                <fmt:message key="function.description.endless.waiting.process" />
            </p></li>
        <li><p>
                <a href="infiniteloop" target="_blank"><fmt:message key="function.name.infinite.loop" /></a>:
                <fmt:message key="function.description.infinite.loop" />
            </p></li>
        <li><p>
                <a href="redirectloop" target="_blank"><fmt:message key="function.name.redirect.loop" /></a>:
                <fmt:message key="function.description.redirect.loop" />
            </p></li>
        <li><p>
                <a href="forwardloop" target="_blank"><fmt:message key="function.name.forward.loop" /></a>:
                <fmt:message key="function.description.forward.loop" />
            </p></li>
        <li><p>
                <a href="jvmcrasheav" target="_blank"><fmt:message key="function.name.jvm.crash.eav" /> </a>:
                <fmt:message key="function.description.jvm.crash.eav" />
            </p></li>
        <li><p>
                <a href="memoryleak"><fmt:message key="function.name.memory.leak" /></a>:
                <fmt:message key="function.description.memory.leak" />
            </p></li>
        <li><p>
                <a href="memoryleak2"> <fmt:message key="function.name.memory.leak2">
                        <fmt:param value="<%=permName%>" />
                    </fmt:message>
                </a>:
                <fmt:message key="function.description.memory.leak2">
                    <fmt:param value="<%=permName%>" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="memoryleak3"><fmt:message key="function.name.memory.leak3" /></a>:
                <fmt:message key="function.description.memory.leak3" />
            </p></li>
        <li><p>
                <a href="netsocketleak"><fmt:message key="function.name.network.socket.leak" /></a>:
                <fmt:message key="function.description.network.socket.leak" />
            </p></li>
        <li><p>
                <a href="dbconnectionleak "><fmt:message key="function.name.database.connection.leak" /></a> :
                <fmt:message key="function.description.database.connection.leak" />
            </p></li>
        <li><p>
                <a href="filedescriptorleak "><fmt:message key="function.name.file.descriptor.leak" /></a> :
                <fmt:message key="function.description.file.descriptor.leak" />
            </p></li>
        <li><p>
                <a href="threadleak"><fmt:message key="function.name.thread.leak" /></a>:
                <fmt:message key="function.description.thread.leak" />
            </p></li>
        <li><p>
                <a href="mojibake"><fmt:message key="function.name.mojibake" /></a>:
                <fmt:message key="function.description.mojibake" />
            </p></li>
        <li><p>
                <a href="iof"><fmt:message key="function.name.int.overflow" /></a>:
                <fmt:message key="function.description.int.overflow" />
            </p></li>
        <li><p>
                <a href="roe"><fmt:message key="function.name.round.off.error" /></a>:
                <fmt:message key="function.description.round.off.error" />
            </p></li>
        <li><p>
                <a href="te"><fmt:message key="function.name.truncation.error" /></a>:
                <fmt:message key="function.description.truncation.error" />
            </p></li>
        <li><p>
                <a href="lotd"><fmt:message key="function.name.loss.of.trailing.digits" /></a>:
                <fmt:message key="function.description.loss.of.trailing.digits" />
            </p></li>
    </ul>
    <%
        }
    %>

    <h2>
        <span class="glyphicon glyphicon-knight"></span>&nbsp;
        <fmt:message key="section.vulnerabilities" />
    </h2>
    <p>
        <fmt:message key="description.vulnerabilities" />
    </p>
    <ul>
        <li><p>
                <a href="xss"><fmt:message key="function.name.xss" /></a>:
                <fmt:message key="function.description.xss" />
            </p></li>
        <li><p>
                <a href="sqlijc"><fmt:message key="function.name.sql.injection" /></a>:
                <fmt:message key="function.description.sql.injection" />
            </p></li>
        <li><p>
                <a href="admins/main?logintype=ldapijc"><fmt:message key="function.name.ldap.injection" /></a>:
                <fmt:message key="function.description.ldap.injection" />
            </p></li>
        <li><p>
                <a href="codeijc"><fmt:message key="function.name.code.injection" /></a>:
                <fmt:message key="function.description.code.injection" />
            </p></li>
        <li><p>
                <a href="ognleijc"><fmt:message key="function.name.os.command.injection" /></a>:
                <fmt:message key="function.description.os.command.injection" />
            </p></li>
        <li><p>
                <a href="mailheaderijct"><fmt:message key="function.name.mail.header.injection" /></a>:
                <fmt:message key="function.description.mail.header.injection" />
            </p></li>
        <li><p>
                <a href="nullbyteijct"><fmt:message key="function.name.null.byte.injection" /></a>:
                <fmt:message key="function.description.null.byte.injection" />
            </p></li>
        <li><p>
                <a href="ursupload"><fmt:message key="function.name.unrestricted.size.upload" /></a>:
                <fmt:message key="function.description.unrestricted.size.upload" />
            </p></li>
        <li><p>
                <a href="ureupload"><fmt:message key="function.name.unrestricted.ext.upload" /></a>:
                <fmt:message key="function.description.unrestricted.ext.upload" />
            </p></li>
        <li><p>
                <a href="admins/main?logintype=openredirect&amp;goto=/uid/serverinfo.jsp"><fmt:message key="function.name.open.redirect" /></a>:
                <fmt:message key="function.description.open.redirect" />
            </p></li>
        <li><p>
                <a href="admins/main?logintype=bruteforce"><fmt:message key="function.name.brute.force" /></a>:
                <fmt:message key="function.description.brute.force" />
            </p></li>
        <li><p>
                <a href="<%=response.encodeURL("admins/main?logintype=sessionfixation")%>"><fmt:message key="function.name.session.fixation" /></a>:
                <fmt:message key="function.description.session.fixation" />
            </p></li>
        <li><p>
                <a href="admins/main?logintype=verbosemsg"><fmt:message key="function.name.verbose.error.message" /></a>:
                <fmt:message key="function.description.verbose.error.message" />
            </p></li>
        <li><p>
                <a href="${pageContext.request.contextPath}/dfi/includable.jsp?template=style_bootstrap.html"><fmt:message
                        key="function.name.dangerous.file.inclusion" /></a>:
                <fmt:message key="function.description.dangerous.file.inclusion" />
            </p></li>
        <li><p>
                <a href="${pageContext.request.contextPath}/dt/includable.jsp?template=basic"><fmt:message key="function.name.path.traversal" /></a>:
                <fmt:message key="function.description.path.traversal" />
            </p></li>
        <li><p>
                <a href="${pageContext.request.contextPath}/uid/clientinfo.jsp"><fmt:message key="function.name.unintended.file.disclosure" /></a>:
                <fmt:message key="function.description.unintended.file.disclosure" />
            </p></li>
        <li><p>
                <a href="${pageContext.request.contextPath}/admins/csrf"><fmt:message key="function.name.csrf" /></a>:
                <fmt:message key="function.description.csrf" />
            </p></li>
        <li><p>
                <a href="${pageContext.request.contextPath}/admins/clickjacking"><fmt:message key="function.name.clickjacking" /></a>:
                <fmt:message key="function.description.clickjacking" />
            </p></li>
        <li><p>
                <a href="xee"><fmt:message key="function.name.xee" /></a>:
                <fmt:message key="function.description.xee" />
            </p></li>
        <li><p>
                <a href="xxe"><fmt:message key="function.name.xxe" /></a>:
                <fmt:message key="function.description.xxe" />
            </p></li>
    </ul>

    <%
        if (!isOnlyVulnerabilities) {
    %>
    <h2>
        <span class="glyphicon glyphicon-knight"></span>&nbsp;
        <fmt:message key="section.performance.issue" />
    </h2>
    <p>
        <fmt:message key="description.performance.issue" />
    </p>
    <ul>
        <li><p>
                <a href="slowre"><fmt:message key="function.name.slow.regular.expression" /></a>:
                <fmt:message key="function.description.slow.regular.expression" />
            </p></li>
        <li><p>
                <a href="strplusopr"><fmt:message key="function.name.slow.string.plus.operation" /></a>:
                <fmt:message key="function.description.slow.string.plus.operation" />
            </p></li>
        <li><p>
                <a href="createobjects"><fmt:message key="function.name.slow.unnecessary.object.creation" /></a>:
                <fmt:message key="function.description.slow.unnecessary.object.creation" />
            </p></li>
    </ul>

    <h2>
        <span class="glyphicon glyphicon-knight"></span>&nbsp;
        <fmt:message key="section.errors" />
    </h2>
    <p>
        <fmt:message key="description.errors" />
    </p>
    <ul>
        <li><p>
                <a href="asserr" target="_blank">AssertionError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="AssertionError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="eie" target="_blank"><fmt:message key="function.name.ei.error" /></a>:
                <fmt:message key="function.description.ei.error" />
            </p></li>
        <li><p>
                <a href="fce" target="_blank">FactoryConfigurationError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="FactoryConfigurationError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ncdfe" target="_blank">NoClassDefFoundError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="NoClassDefFoundError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome" target="_blank">OutOfMemoryError (Java heap space)</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome2" target="_blank">OutOfMemoryError (Requested array size exceeds VM limit)</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome3" target="_blank">OutOfMemoryError (unable to create new native thread)</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome4" target="_blank">OutOfMemoryError (GC overhead limit exceeded)</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome5" target="_blank">OutOfMemoryError (<%=permNameInErrorMsg%>)
                </a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="oome6" target="_blank">OutOfMemoryError (Direct buffer memory)</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="OutOfMemoryError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="sofe" target="_blank">StackOverflowError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="StackOverflowError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="tfce" target="_blank">TransformerFactoryConfigurationError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="TransformerFactoryConfigurationError" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="jnicall" target="_blank">UnsatisfiedLinkError</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="UnsatisfiedLinkError" />
                </fmt:message>
            </p></li>
    </ul>

    <h2>
        <span class="glyphicon glyphicon-knight"></span>&nbsp;
        <fmt:message key="section.exceptions" />
    </h2>
    <p>
        <fmt:message key="description.section.exceptions" />
    </p>
    <ul>
        <li><p>
                <a href="ae" target="_blank">ArithmeticException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ArithmeticException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="aioobe" target="_blank">ArrayIndexOutOfBoundsException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ArrayIndexOutOfBoundsException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ase" target="_blank">ArrayStoreException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ArrayStoreException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="boe" target="_blank">BufferOverflowException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="BufferOverflowException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="bue" target="_blank">BufferUnderflowException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="BufferUnderflowException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="cre" target="_blank">CannotRedoException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="CannotRedoException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="cue" target="_blank">CannotUndoException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="CannotUndoException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="cce" target="_blank">ClassCastException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ClassCastException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="cme" target="_blank">ConcurrentModificationException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ConcurrentModificationException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ese" target="_blank">EmptyStackException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="EmptyStackException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="iae" target="_blank">IllegalArgumentException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IllegalArgumentException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="imse" target="_blank">IllegalMonitorStateException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IllegalMonitorStateException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ipse" target="_blank">IllegalPathStateException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IllegalPathStateException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="iase" target="_blank">IllegalStateException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IllegalStateException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="itse" target="_blank">IllegalThreadStateException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IllegalThreadStateException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="imoe" target="_blank">ImagingOpException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="ImagingOpException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ioobe" target="_blank">IndexOutOfBoundsException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="IndexOutOfBoundsException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="ime" target="_blank">InputMismatchException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="InputMismatchException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="mpte" target="_blank">MalformedParameterizedTypeException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="MalformedParameterizedTypeException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="mre" target="_blank">MissingResourceException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="MissingResourceException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="nase" target="_blank">NegativeArraySizeException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="NegativeArraySizeException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="nsee" target="_blank">NoSuchElementException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="NoSuchElementException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="npe" target="_blank">NullPointerException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="NullPointerException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="nfe" target="_blank">NumberFormatException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="NumberFormatException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="se" target="_blank">SecurityException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="SecurityException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="uce" target="_blank">UnsupportedCharsetException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="UnsupportedCharsetException" />
                </fmt:message>
            </p></li>
        <li><p>
                <a href="uoe" target="_blank">UnsupportedOperationException</a>:
                <fmt:message key="function.description.throwable">
                    <fmt:param value="UnsupportedOperationException" />
                </fmt:message>
            </p></li>
    </ul>
    <%
        }
    %>

    <hr>
    <footer>
        <img src="images/easybuggyL.png" />Copyright &copy; 2017 T246 OSS Lab, all rights reserved.<br /> <br />
    </footer>
</body>
</html>