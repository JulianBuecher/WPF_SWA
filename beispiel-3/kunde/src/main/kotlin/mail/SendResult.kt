package com.acme.kunde.mail

import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.MailSendException

/**
 * Resultat beim Senden einer Email.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
sealed class SendResult {
    /**
     * Resultat-Typ, wenn eine Email erfolgreich gesendet wurde.
     */
    object Success : SendResult()

    /**
     * Resultat-Typ, wenn die Email nicht gesendet wurde, weil z.B. der Mailserver nicht erreichbar war.
     * @property exception Die verursachende MailSendException
     */
    data class SendError(val exception: MailSendException) : SendResult()

    /**
     * Resultat-Typ, wenn es einen Authentifizierungsfehler gab.
     * @property exception Die verursachende MailAuthenticationException
     */
    data class AuthenticationError(val exception: MailAuthenticationException) : SendResult()
}
