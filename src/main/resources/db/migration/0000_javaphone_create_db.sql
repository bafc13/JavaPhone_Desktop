BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "attachments" (
	"id"	INTEGER,
	"message_id"	INTEGER,
	"media_id"	INTEGER,
	PRIMARY KEY("id"),
	CONSTRAINT "fKey_media_id" FOREIGN KEY("media_id") REFERENCES "media"("id"),
	CONSTRAINT "fKey_message_id" FOREIGN KEY("message_id") REFERENCES "messages"("id")
);
CREATE TABLE IF NOT EXISTS "chats" (
	"id"	INTEGER,
	"type"	TEXT,
	"host_public_key"	TEXT,
	PRIMARY KEY("id"),
	CONSTRAINT "fKey_host_public_key" FOREIGN KEY("host_public_key") REFERENCES "users"("public_key"),
	CONSTRAINT "chat_type" CHECK("type" IN ('dm', 'server'))
);
CREATE TABLE IF NOT EXISTS "chats_users" (
	"id"	INTEGER,
	"chat_id"	INTEGER,
	"user_public_key"	TEXT,
	PRIMARY KEY("id"),
	CONSTRAINT "fKey_chat_id" FOREIGN KEY("chat_id") REFERENCES "chats"("id"),
	CONSTRAINT "fKey_user_public_key" FOREIGN KEY("user_public_key") REFERENCES "users"("public_key")
)
CREATE TABLE IF NOT EXISTS "media" (
	"id"	INTEGER,
	"path"	TEXT,
	"checksum"	TEXT,
	PRIMARY KEY("id")
);
CREATE TABLE IF NOT EXISTS "messages" (
	"id"	INTEGER,
	"chat_id"	INTEGER,
	"sender_public_key"	TEXT,
	"content"	TEXT,
	"time"	INTEGER,
	PRIMARY KEY("id"),
	CONSTRAINT "fKey_chat_id" FOREIGN KEY("chat_id") REFERENCES "chats"("id"),
	CONSTRAINT "fKey_sender_public_key" FOREIGN KEY("sender_public_key") REFERENCES "users"("public_key")
);
CREATE TABLE IF NOT EXISTS "users" (
	"public_key"	TEXT,
	"name"	TEXT,
	"email"	TEXT,
	"ip"	TEXT,
	"avatar_id"	INTEGER,
	PRIMARY KEY("public_key"),
	CONSTRAINT "fKey_avatar_id" FOREIGN KEY("avatar_id") REFERENCES "media"("id")
);
COMMIT;
