# classes6.dex

.class public final Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;
.super Ljava/lang/Object;
.source "ToastViewBinding.java"

# interfaces
.implements Landroidx/viewbinding/ViewBinding;


# instance fields
.field public final icon:Lcom/google/android/material/imageview/ShapeableImageView;

.field private final rootView:Lcom/google/android/material/card/MaterialCardView;

.field public final textView:Lcom/google/android/material/textview/MaterialTextView;


# direct methods
.method private constructor <init>(Lcom/google/android/material/card/MaterialCardView;Lcom/google/android/material/imageview/ShapeableImageView;Lcom/google/android/material/textview/MaterialTextView;)V
    .registers 4
    .param p1, "rootView"  # Lcom/google/android/material/card/MaterialCardView;
    .param p2, "icon"  # Lcom/google/android/material/imageview/ShapeableImageView;
    .param p3, "textView"  # Lcom/google/android/material/textview/MaterialTextView;

    .line 30
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 31
    iput-object p1, p0, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->rootView:Lcom/google/android/material/card/MaterialCardView;

    .line 32
    iput-object p2, p0, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->icon:Lcom/google/android/material/imageview/ShapeableImageView;

    .line 33
    iput-object p3, p0, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->textView:Lcom/google/android/material/textview/MaterialTextView;

    .line 34
    return-void
.end method

.method public static bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;
    .registers 6
    .param p0, "rootView"  # Landroid/view/View;

    .line 63
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->icon:I

    .line 64
    .local v0, "id":I
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Lcom/google/android/material/imageview/ShapeableImageView;

    .line 65
    .local v1, "icon":Lcom/google/android/material/imageview/ShapeableImageView;
    if-eqz v1, :cond_1e

    .line 69
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->text_view:I

    .line 70
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v2

    check-cast v2, Lcom/google/android/material/textview/MaterialTextView;

    .line 71
    .local v2, "textView":Lcom/google/android/material/textview/MaterialTextView;
    if-eqz v2, :cond_1d

    .line 75
    new-instance v3, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;

    move-object v4, p0

    check-cast v4, Lcom/google/android/material/card/MaterialCardView;

    invoke-direct {v3, v4, v1, v2}, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;-><init>(Lcom/google/android/material/card/MaterialCardView;Lcom/google/android/material/imageview/ShapeableImageView;Lcom/google/android/material/textview/MaterialTextView;)V

    return-object v3

    .line 72
    :cond_1d
    goto :goto_1f

    .line 66
    .end local v2  # "textView":Lcom/google/android/material/textview/MaterialTextView;
    :cond_1e
    nop

    .line 77
    .end local v1  # "icon":Lcom/google/android/material/imageview/ShapeableImageView;
    :goto_1f
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object v1

    .line 78
    .local v1, "missingId":Ljava/lang/String;
    new-instance v2, Ljava/lang/NullPointerException;

    const-string v3, "Missing required view with ID: "

    invoke-virtual {v3, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw v2
.end method

.method public static inflate(Landroid/view/LayoutInflater;)Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;
    .registers 3
    .param p0, "inflater"  # Landroid/view/LayoutInflater;

    .line 44
    const/4 v0, 0x0

    const/4 v1, 0x0

    invoke-static {p0, v0, v1}, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;

    move-result-object v0

    return-object v0
.end method

.method public static inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;
    .registers 5
    .param p0, "inflater"  # Landroid/view/LayoutInflater;
    .param p1, "parent"  # Landroid/view/ViewGroup;
    .param p2, "attachToParent"  # Z

    .line 50
    sget v0, Landroid/zero/studio/compose/preview/resources/R$layout;->toast_view:I

    const/4 v1, 0x0

    invoke-virtual {p0, v0, p1, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    .line 51
    .local v0, "root":Landroid/view/View;
    if-eqz p2, :cond_c

    .line 52
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    .line 54
    :cond_c
    invoke-static {v0}, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;

    move-result-object v1

    return-object v1
.end method


# virtual methods
.method public bridge synthetic getRoot()Landroid/view/View;
    .registers 2

    .line 19
    invoke-virtual {p0}, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->getRoot()Lcom/google/android/material/card/MaterialCardView;

    move-result-object v0

    return-object v0
.end method

.method public getRoot()Lcom/google/android/material/card/MaterialCardView;
    .registers 2

    .line 39
    iget-object v0, p0, Landroid/zero/studio/compose/preview/resources/databinding/ToastViewBinding;->rootView:Lcom/google/android/material/card/MaterialCardView;

    return-object v0
.end method
