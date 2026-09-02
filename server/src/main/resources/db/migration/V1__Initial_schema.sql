--
-- PostgreSQL database dump
--

\restrict nvaU4p6umXzzhuWViSyeZvZ3qPuN1MZYg8DEL3NJS3bzKnhHgfCO0tcUC90iPvf

-- Dumped from database version 18.2 (Postgres.app)
-- Dumped by pg_dump version 18.2 (Postgres.app)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: decants; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.decants (
    id integer NOT NULL,
    fragrance_id integer NOT NULL,
    seller_id integer NOT NULL,
    size numeric(6,2) NOT NULL,
    unit character varying(10) DEFAULT 'ML'::character varying NOT NULL,
    price numeric(10,2) NOT NULL,
    stock_quantity integer DEFAULT 1 NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.decants OWNER TO postgres;

--
-- Name: decants_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.decants_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.decants_id_seq OWNER TO postgres;

--
-- Name: decants_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.decants_id_seq OWNED BY public.decants.id;


--
-- Name: follows; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.follows (
    follower_id integer NOT NULL,
    following_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.follows OWNER TO postgres;

--
-- Name: fragrance_media; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fragrance_media (
    fragrance_id integer NOT NULL,
    media_item_id integer NOT NULL
);


ALTER TABLE public.fragrance_media OWNER TO postgres;

--
-- Name: fragrance_notes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fragrance_notes (
    fragrance_id integer NOT NULL,
    note character varying(100) NOT NULL,
    note_type character varying(10) NOT NULL
);


ALTER TABLE public.fragrance_notes OWNER TO postgres;

--
-- Name: fragrances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fragrances (
    id integer NOT NULL,
    seller_id integer NOT NULL,
    name character varying(150) NOT NULL,
    brand character varying(100) NOT NULL,
    description text,
    price numeric(10,2) NOT NULL,
    volume_ml integer,
    concentration character varying(30),
    condition character varying(20) NOT NULL,
    stock_quantity integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    view_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.fragrances OWNER TO postgres;

--
-- Name: fragrances_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.fragrances_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.fragrances_id_seq OWNER TO postgres;

--
-- Name: fragrances_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.fragrances_id_seq OWNED BY public.fragrances.id;


--
-- Name: listing_media; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.listing_media (
    listing_id integer NOT NULL,
    media_item_id integer NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE public.listing_media OWNER TO postgres;

--
-- Name: listings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.listings (
    id integer NOT NULL,
    seller_id integer NOT NULL,
    fragrance_id integer NOT NULL,
    price numeric(10,2) NOT NULL,
    condition character varying(20) NOT NULL,
    is_negotiable boolean DEFAULT false NOT NULL,
    stock_quantity integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    deleted_at timestamp without time zone,
    kind character varying(16),
    nominal_size_ml integer,
    remaining_ml integer,
    fill_source character varying(16),
    fill_confidence double precision
);


ALTER TABLE public.listings OWNER TO postgres;

--
-- Name: listings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.listings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.listings_id_seq OWNER TO postgres;

--
-- Name: listings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.listings_id_seq OWNED BY public.listings.id;


--
-- Name: media_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.media_items (
    id integer NOT NULL,
    uploader_id integer NOT NULL,
    type character varying(10) NOT NULL,
    url character varying(500) NOT NULL,
    thumbnail_url character varying(500),
    duration_seconds integer,
    caption text,
    like_count integer DEFAULT 0 NOT NULL,
    comment_count integer DEFAULT 0 NOT NULL,
    is_review boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone NOT NULL,
    cloudflare_uid character varying(64),
    cf_upload_status character varying(10) DEFAULT 'PENDING'::character varying NOT NULL
);


ALTER TABLE public.media_items OWNER TO postgres;

--
-- Name: media_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.media_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.media_items_id_seq OWNER TO postgres;

--
-- Name: media_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.media_items_id_seq OWNED BY public.media_items.id;


--
-- Name: media_likes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.media_likes (
    user_id integer NOT NULL,
    media_item_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.media_likes OWNER TO postgres;

--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    id integer NOT NULL,
    buyer_id integer NOT NULL,
    seller_id integer NOT NULL,
    fragrance_id integer,
    decant_id integer,
    quantity integer DEFAULT 1 NOT NULL,
    total_amount numeric(10,2) NOT NULL,
    status character varying(20) NOT NULL,
    shipping_address text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.orders_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orders_id_seq OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: post_fragrances; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post_fragrances (
    post_id integer NOT NULL,
    fragrance_id integer NOT NULL
);


ALTER TABLE public.post_fragrances OWNER TO postgres;

--
-- Name: post_hashtags; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post_hashtags (
    post_id integer NOT NULL,
    hashtag character varying(50) NOT NULL
);


ALTER TABLE public.post_hashtags OWNER TO postgres;

--
-- Name: post_likes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post_likes (
    user_id integer NOT NULL,
    post_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.post_likes OWNER TO postgres;

--
-- Name: post_listings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post_listings (
    id integer NOT NULL,
    post_id integer NOT NULL,
    fragrance_id integer NOT NULL,
    price numeric(10,2) NOT NULL,
    condition character varying(50) NOT NULL,
    is_negotiable boolean DEFAULT false NOT NULL
);


ALTER TABLE public.post_listings OWNER TO postgres;

--
-- Name: post_listings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.post_listings_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.post_listings_id_seq OWNER TO postgres;

--
-- Name: post_listings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.post_listings_id_seq OWNED BY public.post_listings.id;


--
-- Name: post_media; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.post_media (
    id integer NOT NULL,
    post_id integer NOT NULL,
    url character varying(500) NOT NULL,
    index integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.post_media OWNER TO postgres;

--
-- Name: post_media_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.post_media_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.post_media_id_seq OWNER TO postgres;

--
-- Name: post_media_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.post_media_id_seq OWNED BY public.post_media.id;


--
-- Name: posts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.posts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    content_format character varying(20) NOT NULL,
    text_content text,
    like_count integer DEFAULT 0 NOT NULL,
    comment_count integer DEFAULT 0 NOT NULL,
    share_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.posts OWNER TO postgres;

--
-- Name: posts_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.posts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.posts_id_seq OWNER TO postgres;

--
-- Name: posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.posts_id_seq OWNED BY public.posts.id;


--
-- Name: reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reviews (
    id integer NOT NULL,
    reviewer_id integer NOT NULL,
    fragrance_id integer NOT NULL,
    order_id integer,
    rating integer NOT NULL,
    content text,
    media_item_id integer,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.reviews OWNER TO postgres;

--
-- Name: reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reviews_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reviews_id_seq OWNER TO postgres;

--
-- Name: reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.reviews_id_seq OWNED BY public.reviews.id;


--
-- Name: user_fragrance_collection; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_fragrance_collection (
    user_id integer NOT NULL,
    fragrance_id integer NOT NULL,
    added_at timestamp without time zone NOT NULL,
    personal_notes text,
    bottle_size_ml integer,
    status character varying(20) NOT NULL
);


ALTER TABLE public.user_fragrance_collection OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    password_hash character varying(255),
    google_id character varying(255),
    apple_id character varying(255),
    display_name character varying(100) NOT NULL,
    avatar_url character varying(255),
    bio text,
    is_seller boolean DEFAULT false NOT NULL,
    follower_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: decants id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.decants ALTER COLUMN id SET DEFAULT nextval('public.decants_id_seq'::regclass);


--
-- Name: fragrances id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrances ALTER COLUMN id SET DEFAULT nextval('public.fragrances_id_seq'::regclass);


--
-- Name: listings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listings ALTER COLUMN id SET DEFAULT nextval('public.listings_id_seq'::regclass);


--
-- Name: media_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_items ALTER COLUMN id SET DEFAULT nextval('public.media_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: post_listings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_listings ALTER COLUMN id SET DEFAULT nextval('public.post_listings_id_seq'::regclass);


--
-- Name: post_media id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_media ALTER COLUMN id SET DEFAULT nextval('public.post_media_id_seq'::regclass);


--
-- Name: posts id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.posts ALTER COLUMN id SET DEFAULT nextval('public.posts_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews ALTER COLUMN id SET DEFAULT nextval('public.reviews_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: decants decants_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.decants
    ADD CONSTRAINT decants_pkey PRIMARY KEY (id);


--
-- Name: fragrances fragrances_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrances
    ADD CONSTRAINT fragrances_pkey PRIMARY KEY (id);


--
-- Name: listings listings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT listings_pkey PRIMARY KEY (id);


--
-- Name: media_items media_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_items
    ADD CONSTRAINT media_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: follows pk_follows; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT pk_follows PRIMARY KEY (follower_id, following_id);


--
-- Name: fragrance_media pk_fragrance_media; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrance_media
    ADD CONSTRAINT pk_fragrance_media PRIMARY KEY (fragrance_id, media_item_id);


--
-- Name: listing_media pk_listing_media; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listing_media
    ADD CONSTRAINT pk_listing_media PRIMARY KEY (listing_id, media_item_id);


--
-- Name: media_likes pk_media_likes; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_likes
    ADD CONSTRAINT pk_media_likes PRIMARY KEY (user_id, media_item_id);


--
-- Name: post_fragrances pk_post_fragrances; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_fragrances
    ADD CONSTRAINT pk_post_fragrances PRIMARY KEY (post_id, fragrance_id);


--
-- Name: post_hashtags pk_post_hashtags; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_hashtags
    ADD CONSTRAINT pk_post_hashtags PRIMARY KEY (post_id, hashtag);


--
-- Name: post_likes pk_post_likes; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT pk_post_likes PRIMARY KEY (user_id, post_id);


--
-- Name: user_fragrance_collection pk_user_fragrance_collection; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_fragrance_collection
    ADD CONSTRAINT pk_user_fragrance_collection PRIMARY KEY (user_id, fragrance_id);


--
-- Name: post_listings post_listings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_listings
    ADD CONSTRAINT post_listings_pkey PRIMARY KEY (id);


--
-- Name: post_media post_media_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_media
    ADD CONSTRAINT post_media_pkey PRIMARY KEY (id);


--
-- Name: posts posts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: users users_apple_id_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_apple_id_unique UNIQUE (apple_id);


--
-- Name: users users_email_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_unique UNIQUE (email);


--
-- Name: users users_google_id_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_google_id_unique UNIQUE (google_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_unique; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_unique UNIQUE (username);


--
-- Name: decants fk_decants_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.decants
    ADD CONSTRAINT fk_decants_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: decants fk_decants_seller_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.decants
    ADD CONSTRAINT fk_decants_seller_id__id FOREIGN KEY (seller_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: follows fk_follows_follower_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT fk_follows_follower_id__id FOREIGN KEY (follower_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: follows fk_follows_following_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.follows
    ADD CONSTRAINT fk_follows_following_id__id FOREIGN KEY (following_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: fragrance_media fk_fragrance_media_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrance_media
    ADD CONSTRAINT fk_fragrance_media_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: fragrance_media fk_fragrance_media_media_item_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrance_media
    ADD CONSTRAINT fk_fragrance_media_media_item_id__id FOREIGN KEY (media_item_id) REFERENCES public.media_items(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: fragrance_notes fk_fragrance_notes_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrance_notes
    ADD CONSTRAINT fk_fragrance_notes_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: fragrances fk_fragrances_seller_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fragrances
    ADD CONSTRAINT fk_fragrances_seller_id__id FOREIGN KEY (seller_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: listing_media fk_listing_media_listing_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listing_media
    ADD CONSTRAINT fk_listing_media_listing_id__id FOREIGN KEY (listing_id) REFERENCES public.listings(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: listing_media fk_listing_media_media_item_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listing_media
    ADD CONSTRAINT fk_listing_media_media_item_id__id FOREIGN KEY (media_item_id) REFERENCES public.media_items(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: listings fk_listings_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT fk_listings_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: listings fk_listings_seller_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.listings
    ADD CONSTRAINT fk_listings_seller_id__id FOREIGN KEY (seller_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: media_items fk_media_items_uploader_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_items
    ADD CONSTRAINT fk_media_items_uploader_id__id FOREIGN KEY (uploader_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: media_likes fk_media_likes_media_item_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_likes
    ADD CONSTRAINT fk_media_likes_media_item_id__id FOREIGN KEY (media_item_id) REFERENCES public.media_items(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: media_likes fk_media_likes_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.media_likes
    ADD CONSTRAINT fk_media_likes_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_buyer_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_buyer_id__id FOREIGN KEY (buyer_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_decant_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_decant_id__id FOREIGN KEY (decant_id) REFERENCES public.decants(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_seller_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_seller_id__id FOREIGN KEY (seller_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_fragrances fk_post_fragrances_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_fragrances
    ADD CONSTRAINT fk_post_fragrances_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_fragrances fk_post_fragrances_post_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_fragrances
    ADD CONSTRAINT fk_post_fragrances_post_id__id FOREIGN KEY (post_id) REFERENCES public.posts(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_hashtags fk_post_hashtags_post_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_hashtags
    ADD CONSTRAINT fk_post_hashtags_post_id__id FOREIGN KEY (post_id) REFERENCES public.posts(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_likes fk_post_likes_post_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT fk_post_likes_post_id__id FOREIGN KEY (post_id) REFERENCES public.posts(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_likes fk_post_likes_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_likes
    ADD CONSTRAINT fk_post_likes_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_listings fk_post_listings_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_listings
    ADD CONSTRAINT fk_post_listings_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_listings fk_post_listings_post_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_listings
    ADD CONSTRAINT fk_post_listings_post_id__id FOREIGN KEY (post_id) REFERENCES public.posts(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: post_media fk_post_media_post_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.post_media
    ADD CONSTRAINT fk_post_media_post_id__id FOREIGN KEY (post_id) REFERENCES public.posts(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: posts fk_posts_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.posts
    ADD CONSTRAINT fk_posts_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: reviews fk_reviews_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk_reviews_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: reviews fk_reviews_media_item_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk_reviews_media_item_id__id FOREIGN KEY (media_item_id) REFERENCES public.media_items(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: reviews fk_reviews_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk_reviews_order_id__id FOREIGN KEY (order_id) REFERENCES public.orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: reviews fk_reviews_reviewer_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT fk_reviews_reviewer_id__id FOREIGN KEY (reviewer_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_fragrance_collection fk_user_fragrance_collection_fragrance_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_fragrance_collection
    ADD CONSTRAINT fk_user_fragrance_collection_fragrance_id__id FOREIGN KEY (fragrance_id) REFERENCES public.fragrances(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: user_fragrance_collection fk_user_fragrance_collection_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_fragrance_collection
    ADD CONSTRAINT fk_user_fragrance_collection_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict nvaU4p6umXzzhuWViSyeZvZ3qPuN1MZYg8DEL3NJS3bzKnhHgfCO0tcUC90iPvf

