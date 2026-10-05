package com.operationstower.platform.security;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class GroupAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  static final String GROUPS_CLAIM = "groups";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<String> groups = jwt.getClaimAsStringList(GROUPS_CLAIM);
    if (groups == null) {
      return List.of();
    }
    return groups.stream().map(GroupAuthoritiesConverter::toRole).toList();
  }

  private static GrantedAuthority toRole(String group) {
    String normalized = group.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    return new SimpleGrantedAuthority("ROLE_" + normalized);
  }
}
