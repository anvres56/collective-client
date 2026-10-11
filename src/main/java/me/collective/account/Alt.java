package me.collective.account;

/** Оффлайн-аккаунт: ник + UUID. Microsoft-логин не поддерживается (для него есть IAS). */
public record Alt(String name, String uuid) {
}
