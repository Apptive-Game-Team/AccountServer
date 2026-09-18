-- refresh_token_member_id_fkey (V002) has no ON DELETE behavior, so deleting a member who has
-- ever logged in fails with a foreign key violation instead of removing their tokens.
--
-- A refresh token only exists as an attribute of its member: once the member is gone, the token
-- can only ever be presented for a session that no longer exists. Cascading the delete removes
-- the dead rows instead of blocking MemberService.deleteMember().
--
-- member_authority, key_value and system.created_by have the same shape but are handled by a
-- separate change: system.created_by must not cascade, since deleting a member should not delete
-- the systems that member created.

ALTER TABLE ONLY public.refresh_token
    DROP CONSTRAINT refresh_token_member_id_fkey;

ALTER TABLE ONLY public.refresh_token
    ADD CONSTRAINT refresh_token_member_id_fkey FOREIGN KEY (member_id)
        REFERENCES public.member(id) ON DELETE CASCADE;
