package com.github.sszuev.ontconverter.utils

import com.github.owlcs.ontapi.OntFormat
import kotlin.streams.toList

fun supportedReadFormats(): List<OntFormat> {
    return OntFormat.formats().filter(OntFormat::isReadSupported).toList()
}

fun supportedWriteFormats(): List<OntFormat> {
    return OntFormat.formats().filter(OntFormat::isWriteSupported).toList()
}

