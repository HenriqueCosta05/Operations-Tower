package com.operationstower.users.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AuthentikUserPage(List<AuthentikUserDto> results) {}
