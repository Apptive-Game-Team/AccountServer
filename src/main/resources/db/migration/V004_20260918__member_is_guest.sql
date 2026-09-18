-- member.is_guest records whether an account is still a guest.
--
-- AuthenticationService.joinGuest() creates an account with a generated email and a password the
-- user never chooses and never sees. Until now the client could only tell a guest apart by
-- holding that generated password in memory, so a login restored after a restart looked like a
-- real member: the client hid the button that converts the account, and the guest could never
-- recover it, knowing neither the email nor the password. This column is what the server reads
-- instead, and it rides on the access token as the `guest` claim.
--
-- The backfill below is the only place the guest email shape is ever used as evidence. Rows that
-- predate this column carry nothing else, so they are classified once here; from this migration
-- on the column is the authority and no code infers guestness from an email. The backslash
-- escapes the underscore, which LIKE would otherwise read as a single-character wildcard.
--
-- MemberService.putMember() clears the flag when a guest finishes converting, so a converted
-- account keeps its guest-shaped email while no longer counting as a guest.

--
-- Columns
--

ALTER TABLE ONLY public.member
    ADD COLUMN is_guest boolean DEFAULT false NOT NULL;

--
-- Backfill
--

UPDATE public.member
    SET is_guest = true
    WHERE email LIKE 'guest\_%@example.com';
